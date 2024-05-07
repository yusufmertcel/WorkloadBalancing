package BalancingDemo1

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class BalancingStep {
    private Balancing balancing
    private BalancingStep previousStep
    private Combination selectedCombination
    private List<CombinationStepValues> combinationSteps
    private int stepNumber
    private int week = 0
    private Issue assignedIssue

    BalancingStep(Balancing balancing, BalancingStep prev){
        this.balancing = balancing
        previousStep = prev
    }

    Combination getMaxDiffCombination(){
        List<CombinationStepValues> combStepValues = getCombinationSteps()
        if(combStepValues.size() > 0){
            def combStep= combStepValues.findAll{cs ->
                cs.RestAmount() > 0 }.max {it.Difference()}
            //def combStep = combStepValues.find{cs -> cs.RestAmount() > 0 && cs.Difference() == maxDiff}
            if(combStep == null)
                return combStepValues.find().combination
            else
                return combStep.combination
        }
    }

    int getStepNumber(){
        if( previousStep == null )
            stepNumber = 1
        else {
            stepNumber = previousStep.stepNumber + 1
        }
        stepNumber
    }


    List<CombinationStepValues> getCombinationSteps(){
        if(combinationSteps == null){
            combinationSteps = new ArrayList<CombinationStepValues>()
            for(def comb in balancing.combinations){
                combinationSteps.add(new CombinationStepValues(this, comb))
            }
        }
        return combinationSteps
    }

    String toString(){
        this.stepNumber
    }
}
