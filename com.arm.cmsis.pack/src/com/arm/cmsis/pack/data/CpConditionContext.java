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

package com.arm.cmsis.pack.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.arm.cmsis.pack.enums.EEvaluationResult;
import com.arm.cmsis.pack.generic.Attributes;

/**
 *  Default implementation of ICpConditionContext interface
 */
public class CpConditionContext extends Attributes implements ICpConditionContext {

	protected EEvaluationResult fResult = EEvaluationResult.IGNORED;
	protected Map<ICpItem, EEvaluationResult> fResults = null;
	
	// temporary variables
	protected Set<ICpCondition> tConditionsBeingEvaluated = new HashSet<ICpCondition>(); // to prevent recursion
	protected EEvaluationResult tResultAccept = EEvaluationResult.UNDEFINED; // keeps last (the best) accept result
	protected boolean tbDeny = false; // flag is set when deny expression is evaluated  
	
	public CpConditionContext() {
	}
	
	@Override
	public void resetResult() {
		fResult = EEvaluationResult.IGNORED;
		fResults = null;
		tResultAccept = EEvaluationResult.UNDEFINED;
		tbDeny = false;
		tConditionsBeingEvaluated.clear();
	}

	@Override
	public EEvaluationResult getEvaluationResult() {
		return fResult;
	}

	
	@Override
	public void setEvaluationResult(EEvaluationResult result) {
		fResult = result;
	}

	@Override
	public EEvaluationResult getEvaluationResult(ICpItem item) {
		if(fResults != null)
			return fResults.get(item);
		return null;
	}

	@Override
	public EEvaluationResult evaluate(ICpItem item) {
		if(item == null)
			return EEvaluationResult.IGNORED;
		EEvaluationResult res = EEvaluationResult.UNDEFINED;
		if(fResults == null)
			fResults = new HashMap<ICpItem, EEvaluationResult>();

		res = getCachedResult(item);
		if(res == null || res == EEvaluationResult.UNDEFINED) {
			res = item.evaluate(this);
			putCachedResult(item, res);
		}
		return res;
	}

	
	 /**
	 * Retrieves cached result for the given item if already in cache
	 * @param item ICpItem for which to retrieve result
	 * @return cached result or null if not yet in cache
	 */
	protected EEvaluationResult getCachedResult(ICpItem item) {
		 if(fResults != null)
			 return fResults.get(item);
		 return null;
	 }

	 /**
	 * Puts evaluation result into cache
	 * @param item ICpItem for which to put the result
	 * @param res result value to cache
	 */
	protected void putCachedResult(ICpItem item, EEvaluationResult res) {
		 if(fResults == null)
			 fResults = new HashMap<ICpItem, EEvaluationResult>();
		fResults.put(item, res);
	 }

	 
	@Override
	public EEvaluationResult evaluateExpression(ICpExpression expression) {
		if(expression == null)
			return EEvaluationResult.IGNORED;
		switch(expression.getExpressionDomain()) {
		case ICpExpression.COMPONENT_EXPRESSION:
			return EEvaluationResult.IGNORED;
		case ICpExpression.DEVICE_EXPRESSION:
		case ICpExpression.TOOLCHAIN_EXPRESSION:
			boolean b = matchCommonAttributes(expression.attributes()); 
			return  b ? EEvaluationResult.FULFILLED : EEvaluationResult.FAILED;
		case ICpExpression.REFERENCE_EXPRESSION:
			return evaluate(expression.getCondition()); 
		default: 
			break;
		}
		return EEvaluationResult.ERROR;
	}

	@Override
	public EEvaluationResult evaluateCondition(ICpCondition condition) {
		if(tConditionsBeingEvaluated.contains(condition))
			return EEvaluationResult.ERROR; // recursion
		
		tConditionsBeingEvaluated.add(condition);
		EEvaluationResult resultRequire = EEvaluationResult.IGNORED;
		EEvaluationResult resultAccept = EEvaluationResult.UNDEFINED;
		// first check require and deny expressions
		Collection<? extends ICpItem> children = condition.getChildren();
		for(ICpItem child :  children) {
			if(!(child instanceof ICpExpression))
				continue;
			ICpExpression expr = (ICpExpression)child;
			boolean bDeny = tbDeny; // save deny context  
			if(expr.getExpressionType() == ICpExpression.DENY_EXPRESSION)
				tbDeny = !tbDeny; // invert the deny context 
			EEvaluationResult res =  evaluate(expr);
			tbDeny = bDeny; // restore deny context
			if(res == EEvaluationResult.IGNORED || res == EEvaluationResult.UNDEFINED )
				continue;
			else if(res == EEvaluationResult.ERROR)
				return res;
			if(expr.getExpressionType() == ICpExpression.ACCEPT_EXPRESSION) {
				if(res.ordinal() > resultAccept.ordinal()){
					resultAccept = res;
				}
			} else {
				if(res.ordinal() < resultRequire.ordinal()){
					resultRequire = res;
				}	
			}
		}

		tConditionsBeingEvaluated.remove(condition);

		tResultAccept = resultAccept; 
		if(resultAccept != EEvaluationResult.UNDEFINED && 
		   resultAccept.ordinal() < resultRequire.ordinal()) {  
			return resultAccept;
		}
		
		return resultRequire;
	}


	@Override
	public Collection<ICpItem> filterItems(Collection<? extends ICpItem> sourceCollection) {
		Collection<ICpItem> filtered = new LinkedList<ICpItem>();
		if(sourceCollection != null && ! sourceCollection.isEmpty()) {
			for(ICpItem item : sourceCollection) {
				EEvaluationResult res = item.evaluate(this);
				if(res.isFulfilled())
					filtered.add(item);
			}
		}
		return filtered;		
	}
	
}
