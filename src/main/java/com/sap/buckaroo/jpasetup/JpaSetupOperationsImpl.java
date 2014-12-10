package com.sap.buckaroo.jpasetup;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Shell;


@Component
@Service
public class JpaSetupOperationsImpl implements JpaSetupOperations {

	
	@Reference private Shell shell;

	@Override
	public void run() {
		shell.executeCommand("jpa setup --provider ECLIPSELINK --database HYPERSONIC_IN_MEMORY");
	}
}
