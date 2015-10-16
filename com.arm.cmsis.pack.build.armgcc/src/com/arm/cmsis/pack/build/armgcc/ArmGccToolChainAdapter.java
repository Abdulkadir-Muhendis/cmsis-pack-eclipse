/*******************************************************************************
* Copyright (c) 2015 ARM Ltd. and others
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* ARM Ltd and ARM Germany GmbH - Initial API and implementation
*******************************************************************************/

package com.arm.cmsis.pack.build.armgcc;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;

import com.arm.cmsis.pack.build.settings.IBuildSettings;
import com.arm.cmsis.pack.build.settings.RteToolChainAdapter;
import com.arm.cmsis.pack.common.CmsisConstants;

/**
 * Toolchain adapter for ARMGCC bare metal toolchain
 *
 */
public class ArmGccToolChainAdapter extends RteToolChainAdapter {
	
	public static final String SPACE 		= " ";  			//$NON-NLS-1$
	public static final String EQUAL 		= "=";  			//$NON-NLS-1$
	public static final String MTHUMB 		= "-mthumb"; 		//$NON-NLS-1$ 
	public static final String MCPU 		= "-mcpu"; 			//$NON-NLS-1$
	public static final String MFPU 		= "-mfpu"; 			//$NON-NLS-1$
	public static final String MFABI 		= "-mfloat-abi"; 	//$NON-NLS-1$
	
	public ArmGccToolChainAdapter() {
	}
	
	@Override
	protected int getRteOptionType(String id) {
		switch(id) {
		case "com.arm.eclipse.cdt.managedbuild.ds5.gcc.baremetal.tool.assembler.option.cpu":  	//$NON-NLS-1$
		case "com.arm.eclipse.cdt.managedbuild.ds5.gcc.baremetal.tool.c.compiler.option.cpu": 	//$NON-NLS-1$
			return CPU_OPTION;
		case "com.arm.eclipse.cdt.managedbuild.ds5.gcc.baremetal.tool.assembler.option.fpu": 	//$NON-NLS-1$
		case "com.arm.eclipse.cdt.managedbuild.ds5.gcc.baremetal.tool.c.compiler.option.fpu":	//$NON-NLS-1$
			return FPU_OPTION;
		case "com.arm.eclipse.cdt.managedbuild.ds5.gcc.baremetal.tool.assembler.option.fabi": 	//$NON-NLS-1$
		case "com.arm.eclipse.cdt.managedbuild.ds5.gcc.baremetal.tool.c.compiler.option.fabi":	//$NON-NLS-1$
			return FLOAT_ABI_OPTION;
		case "com.arm.eclipse.cdt.managedbuild.ds5.gcc.baremetal.tool.assembler.option.endian": //$NON-NLS-1$
		case "com.arm.eclipse.cdt.managedbuild.ds5.gcc.baremetal.tool.c.compiler.option.endian"://$NON-NLS-1$
			return ENDIAN_OPTION;
		case "com.arm.eclipse.cdt.managedbuild.ds5.gcc.baremetal.tool.assembler.option.thumb": //$NON-NLS-1$;
		case "com.arm.eclipse.cdt.managedbuild.ds5.gcc.baremetal.tool.c.compiler.option.thumb": //$NON-NLS-1$;
			return THUMB_OPTION;
		case "com.arm.eclipse.cdt.managedbuild.ds5.gcc.baremetal.tool.c.compiler.option.other": //$NON-NLS-1$
			return CMISC_OPTION;
		case "com.arm.eclipse.cdt.managedbuild.ds5.gcc.baremetal.tool.assembler.option.other": //$NON-NLS-1$
			return AMISC_OPTION;
		case "com.arm.eclipse.cdt.managedbuild.ds5.gcc.baremetal.tool.c.linker.option.flags": //$NON-NLS-1$
			return LMISC_OPTION;
		case "com.arm.tool.librarion.options.misc": //$NON-NLS-1$
			return ARMISC_OPTION;
		case "com.arm.eclipse.cdt.managedbuild.ds5.gcc.baremetal.tool.c.linker.option.script": 	//$NON-NLS-1$;
			return LINKER_SCRIPT_OPTION;
		
		default: 
			break;
		}
		return UNKNOWN_OPTION;
	}

	
	@Override
	protected void updateRteOption(int oType, IConfiguration configuration, IHoldsOptions tool, IOption option, IBuildSettings buildSettings) throws BuildException {

		switch(oType) {	
		case  LMISC_OPTION:
			updateLinkerMiscOption(configuration, tool, option, buildSettings);
			return;
		default :
			break;
		}
		super.updateRteOption(oType, configuration, tool, option, buildSettings);
	}

	protected void updateLinkerMiscOption(IConfiguration configuration, IHoldsOptions tool, IOption option, IBuildSettings buildSettings) throws BuildException {
		
		String  value = option.getStringValue();
		
		int pos = value.indexOf(MTHUMB);
		if(pos >=0 )
			value = value.substring(0,  pos);
		if(!value.isEmpty() && !value.endsWith(SPACE))
			value += SPACE; 
		value += MTHUMB;

		String cpu = getCpuOptionValue(buildSettings);
		value += SPACE + MCPU + EQUAL + cpu;

		String fpu = getFpuOptionValue(buildSettings);
		if(fpu != null && !fpu.isEmpty()) {
			value += SPACE + MFPU + EQUAL + fpu;
		}
		String floatAbi = getFloatAbiOptionValue(buildSettings);
		if(floatAbi != null && !floatAbi.isEmpty()) {
			value += SPACE + MFABI + EQUAL + floatAbi;
		}
		
		ManagedBuildManager.setOption(configuration, tool, option, value);
	}

	@Override
	protected String getRteOptionValue(int oType, IBuildSettings buildSettings) {
		switch(oType) {
		case CPU_OPTION: 
			return getCpuOptionValue(buildSettings);
		case THUMB_OPTION:
			return "1";  //$NON-NLS-1$
		case ENDIAN_OPTION:
			return null; // bug in DS-5 armgcc toolchain  
		case FPU_OPTION:
			return getFpuOptionValue(buildSettings);
		case FLOAT_ABI_OPTION:
			return getFloatAbiOptionValue(buildSettings);
		default:
			break;
 
		}
		return super.getRteOptionValue(oType, buildSettings);
	}
	
	
	protected String getCpuOptionValue(IBuildSettings buildSettings) {
		String cpu = getDeviceAttribute(CPU_OPTION, buildSettings);
		int pos = cpu.indexOf('+');
		if(pos > 0) {
			// Cortex-M0+ -> Cortex-M0plus 
			cpu = cpu.substring(0,  pos);
			cpu += "plus";  //$NON-NLS-1$
		}
		return cpu.toLowerCase();	
	}



	@Override
	protected Collection<String> getStringListValue( IBuildSettings buildSettings, int type) {
		if(type == IOption.LIBRARIES || type == IOption.LIBRARY_PATHS ) { 
			return null; // we add libraries as objects => ignore libs and lib paths
		} else if(type == IOption.OBJECTS) {
			Collection<String> objs = buildSettings.getStringListValue(type);
			List<String> value = new LinkedList<String>();
			if(objs != null && !objs.isEmpty())
				value.addAll(objs);
			Collection<String> libs = buildSettings.getStringListValue(IOption.LIBRARIES);
			if(libs != null && !libs.isEmpty())
				value.addAll(libs);
			return value;
		}
		return super.getStringListValue(buildSettings, type);
	}


	
	public String getFpuOptionValue(IBuildSettings buildSettings) {
		String cpu = getDeviceAttribute(CPU_OPTION, buildSettings);
		String fpu = getDeviceAttribute(FPU_OPTION, buildSettings);
		if(cpu == null || fpu == null || fpu.equals(CmsisConstants.NO_FPU) || !coreHasFpu(cpu)) 
			return CmsisConstants.EMPTY_STRING;
		if(cpu.equals("Cortex-M7")) { //$NON-NLS-1$
			if(fpu.equals(CmsisConstants.SP_FPU)) 
				return "fpv5-sp-d16"; //$NON-NLS-1$
			if(fpu.equals(CmsisConstants.DP_FPU)) 
				return "fpv5-d16"; //$NON-NLS-1$
		} else if(fpu.equals(CmsisConstants.SP_FPU)) {
			return "fpv4-sp-d16"; //$NON-NLS-1$
		} 
		return null;
	}

	private String getFloatAbiOptionValue(IBuildSettings buildSettings) {
		String cpu = getDeviceAttribute(CPU_OPTION, buildSettings);
		String fpu = getDeviceAttribute(FPU_OPTION, buildSettings);
		if(cpu == null || fpu == null || fpu.equals(CmsisConstants.NO_FPU) || !coreHasFpu(cpu)) 
			return CmsisConstants.EMPTY_STRING;
		return "hard"; //$NON-NLS-1$
	}

}
