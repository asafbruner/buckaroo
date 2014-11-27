package com.sap.river.cds;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Shell;

//Note:  "NOT NULL" is supported on classic types only, not on either of the associations (we could, but it is more work and why do it?)
//CDS types supported: String, integer, decimal, localdate

@Component
@Service
public class CDSOperationsImpl implements CDSOperations {

	private static Logger LOGGER = Logger.getLogger(CDSOperationsImpl.class
			.getName());
	
	@Reference private Shell shell;
	
	private final String entityTmpl = "entity jpa --class ~.domain.%s --testAutomatically";
	private final String fieldAssoc1to1Tmpl = "field reference --fieldName %s --type ~.domain.%s";
	private final String fieldAssoc1toManyTmpl = "field set --fieldName %s --type ~.domain.%s";
	private final String NOT_NULL = "--notNull";
	private final String fieldString = "field string --fieldName %s %s --sizeMin %s --sizeMax %s";   //String only.  The placeholders are  fieldName, notNull, sizeMin value, sizeMax value
	private final String fieldOther = "field %s --fieldName %s %s --type %s";   //All other classic types. The placeholders are  field, fieldName, notNull, and type
	
	//Patterns
	//internal patterns
	private final String ws = "[ \t]*";//unlimited white spaces
	private final String an = "\\w+"; //unlimited alphanumeric chars, no white spaces
	private final String alpha = "[a-zA-Z]*";
	private final String ci = "(?i)";//case insensitive
	//line to ignore (CDS lines that are not relevant to roo)
	private final Pattern pattIgnoreLine = Pattern.compile(ci + "^" + ws + "namespace|^" + ws + "@Schema|^" + ws + "context");
	//Entity line is expected to have the form "entity <entityName> {"  We identify the entity word, and the entityName
	private final Pattern pattEntityLine = Pattern.compile("^" + ws + "entity" + ws + "(" + alpha + ")"); 
	//Field line is expected to be of the form "<fieldname> : <type>"
	private final Pattern pattFieldLine = Pattern.compile("^" + ws + "(" + an + ")" + ws + ":" + ws + "([^;]*)");
	//Association one to one is of the form:  "Association to <entityname>"
	private final Pattern pattFieldAssoc1To1 = Pattern.compile("Association" + ws + "to" + ws + "(" + an + ")");
	//Association one to many is of the form: "Association[?..*] to <entityname>" 
	private final Pattern pattFieldAssoc1toMany = Pattern.compile("Association\\[[0-9]\\.\\.\\*\\]" + ws + "to" + ws + "(" + an + ")");
	//type with "not null" is of the form:  "<type> not null" or "<type>(<size>) not null". 
	//Note: the (not null) section below will REMOVED from the result (see regex positive lookbehind (?=...)), group will only contain its prefix
	private final Pattern pattFieldTypeNotNull = Pattern.compile(ci + "(" + an + "(\\([0-9]*\\))?)" + ws + "(?=" + "not" + ws + "null" + ")");
	//The type can be either of the form "<type>(<number>)" or just "<type>"  (e.g. "Integer" or "String(100) or Decimal(5,2)")
	private final Pattern pattType = Pattern.compile("([A-Za-z]+)" + ws + "(\\(" + ws + "[0-9]+,?[0-9]+?" + ws + "\\))?");
	//the qualifier is of the form (<number>) or (<number>,<number>) with embedded white spaces - trim everything except number section
	private final Pattern pattQualifier = Pattern.compile("[0-9]+,?[0-9]+?");
	//End of entity is expected to be either "};", or just a blank line
	private final Pattern pattEndEntityLine = Pattern.compile("^" + ws + "\\};|^" + ws + "$");
	
	private final String CDSTypeString = "string";
	private final String CDSTypeInteger = "integer";
	private final String CDSTypeDecimal = "decimal";
	private final String CDSTypeLocaldate = "localdate";
	
	//map the CDS field to the Roo field
	@SuppressWarnings("serial")
	private final Map<String, String> convertCDSToRooField = new HashMap<String, String>(){
		{
			put(new String(CDSTypeString), new String("string"));
			put(new String(CDSTypeInteger), new String("number"));
			put(new String(CDSTypeDecimal), new String("number"));
			put(new String(CDSTypeLocaldate), new String("date"));
		}
	};
	//map the CDS field to the Roo type (not all CDS fields have entries here, only those for which Roo needs to define a type)
	@SuppressWarnings("serial")
	private final Map<String, String> convertCDSToRooFieldType = new HashMap<String, String>(){
		{
			put(new String(CDSTypeInteger), new String("java.lang.Integer"));
			put(new String(CDSTypeDecimal), new String("java.lang.Float"));
			put(new String(CDSTypeLocaldate), new String("java.util.Date"));
		}
	};
	
	private final String NEW_LINE = System.getProperty("line.separator");

	/**
	 * the cds file is expected to have the following basic type of format:
	 * 		<few irrelevant lines:  namespace, schema, context>
	 * then a series of entities with fields in the form:
	 * 		entity <name> {
	 * 				<fieldname> : <type or association>
	 * 				...
	 * 		};
	 * with blank lines in between entities
	 * Also, since second pass is not carried out, the entities need to be defined from bottom up (i.e. referenced entities before referencing entities)
	 */
	@Override
	public void parseRunCDS(String cdsFilePathName) {
		LOGGER.info("Generate roo commands from file " + cdsFilePathName);
		
		//validate that the filePathName exists and is a file
		File file = new File(cdsFilePathName);
		if (!file.exists()){
			LOGGER.info("File " + cdsFilePathName + " does not exist");
			return;
		}
		if (file.isDirectory()){
			LOGGER.info("File " + cdsFilePathName + " is a directory");
			return;
		}
		
		StringBuilder sbRooEntityCommands = new StringBuilder();				
		
		//read the file
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(cdsFilePathName));
			String line = null;
			//check each line
			while ((line = br.readLine()) != null){
				Matcher matcherIgnore = pattIgnoreLine.matcher(line);
				if (matcherIgnore.find() == true){//if this is an irrelevant line, move on
					continue;
				}
				
				//entity
				Matcher matcherEntity = pattEntityLine.matcher(line);
				if (matcherEntity.find() == true){
					sbRooEntityCommands.append(String.format(entityTmpl, matcherEntity.group(1))).append(NEW_LINE);
					continue;
				}
				
				//check for end of entity, if so, ignore the line
				Matcher matcherEndEntity = pattEndEntityLine.matcher(line);
				if (matcherEndEntity.find() == true){
					continue;
				}
				
				//field
				Matcher matchFullField = pattFieldLine.matcher(line);
				if (matchFullField.find() == false){
					continue;
				}
				String fieldName = matchFullField.group(1);
				String fieldTypeOrAssociation = matchFullField.group(2);
				
				//now analyze group2, which is the type/association
				Matcher matchAssociation1to1 = pattFieldAssoc1To1.matcher(fieldTypeOrAssociation);
				if (matchAssociation1to1.find() == true){
					//group 1 of matchFullField is field name, group 1 of matchAssociation1to1 is the name of the entity	
					sbRooEntityCommands.append(String.format(fieldAssoc1to1Tmpl, matchFullField.group(1), matchAssociation1to1.group(1))).append(NEW_LINE);
					continue;
				}
				Matcher matchAssociation1toMany = pattFieldAssoc1toMany.matcher(fieldTypeOrAssociation);
				if (matchAssociation1toMany.find() == true){
					//group 1 of matchFullField is field name, group 1 of matchAssociation1toMany is the name of the entity
					sbRooEntityCommands.append(String.format(fieldAssoc1toManyTmpl, matchFullField.group(1), matchAssociation1toMany.group(1))).append(NEW_LINE);
					continue;
				}
				
				//if we are here, this is a type (rather than association)
				String fieldTypeName = fieldTypeOrAssociation;
				//fieldTypeName is the type, e.g. Integer, Integer not null, String (100) not null, etc
				//now determine if it has a "not null" clause
				Matcher matchFieldTypeNotNull = pattFieldTypeNotNull.matcher(fieldTypeName);
				boolean isNotNull = false;
				String typeOnly = null;
				if (matchFieldTypeNotNull.find() == true){
					//group 1 is the field type
					isNotNull = true;
					typeOnly = matchFieldTypeNotNull.group(1);
				}
				else{
					typeOnly = fieldTypeName;
				}
				
				//either way, get the field name, and field qualifier (if the latter exists)
				String cdsField = null, typeQualifier = null;			
				//typeOnly is of form Integer, String(100), or Decimal(5,2)
				Matcher matchFieldType = pattType.matcher(typeOnly);
				if (matchFieldType.find() == true){//this is always true, but we need it anyway
					cdsField = matchFieldType.group(1).toLowerCase();
					typeQualifier = matchFieldType.group(2);
					
				}	
				
				//remove the parentheses surrounding typeQualifier
				if (typeQualifier != null){	
					//typeQualifier is of the form (100) or (6,2)						
					Matcher matchQualifier = pattQualifier.matcher(typeQualifier);
					if (matchQualifier.find() == true){//this is always true, but we need it anyway
						typeQualifier = matchQualifier.group();
					}
				}
					
				//format the field string, and place in result
				if (cdsField.equals(CDSTypeString))
					sbRooEntityCommands.append(String.format(fieldString, fieldName, isNotNull?NOT_NULL:"", 2, typeQualifier)).append(NEW_LINE);
				else
					sbRooEntityCommands.append(String.format(fieldOther, convertCDSToRooField.get(cdsField), fieldName, isNotNull?NOT_NULL:"", convertCDSToRooFieldType.get(cdsField))).append(NEW_LINE);
			}
		}
		
		catch(Exception e){
			LOGGER.info("Exception:  " + cdsFilePathName + ": " + e.toString());
			return;
		}
		finally{
			if (br != null){
				try {
					br.close();
				} catch (IOException e) {
					LOGGER.info("IOException:  " + e.toString());
					return;
				}
			}
		}
		
		//run the lines in a shell roo command, one after the other
		String[] lines = sbRooEntityCommands.toString().split(NEW_LINE);
		for (int ctr=0; ctr<lines.length; ctr++){
			shell.executeCommand(lines[ctr]);
		}
	}
}
