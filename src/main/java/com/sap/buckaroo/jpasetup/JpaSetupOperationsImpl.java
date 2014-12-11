package com.sap.buckaroo.jpasetup;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Shell;

import com.sap.buckaroo.hcp.PersistenceOperationsImpl;


@Component
@Service
public class JpaSetupOperationsImpl implements JpaSetupOperations {

	
	@Reference private Shell shell;
	
	@Override
	public boolean isJpaSetupAvailable(){
		return true;
	}

	@Override
	public void run() {
		shell.executeCommand("jpa setup --provider ECLIPSELINK --database HYPERSONIC_IN_MEMORY");
		
		shell.executeCommand("webapp setup");
		
		//temporarily toggle this flag, in order to allow running of the following command
		PersistenceOperationsImpl.setIsAllowCommandExternally(true);
		shell.executeCommand("hcp setup remote-persistence --database HANA");
		PersistenceOperationsImpl.setIsAllowCommandExternally(false);
	}
}
