package BalancingDemo1

import static java.lang.Integer.MIN_VALUE

class CombinationStepValues {
    private BalancingStep balancingStep
    private Combination combination

    CombinationStepValues(BalancingStep balancingStep, Combination combination){
        this.balancingStep = balancingStep
        this.combination = combination
    }

    private getPreviousCombinationStep(){
        if(balancingStep.previousStep == null ){
            return null
        } else {
            return balancingStep.previousStep.combinationSteps.find {cs ->
                cs.combination == combination }
        }
    }

    int JxQLN(){
        combination.evaluationOfProbabilities * balancingStep.stepNumber
    }

    int FreqRate(){
        int result = 0
        if(balancingStep.selectedCombination){
            for(def value in balancingStep.selectedCombination.values) {
                if(combination.values.contains(value)){
                    result++
                }
            }
        }
        return result
    }

    int CumulativeValue(){
        int result = balancingStep.balancing.issueList.size() * FreqRate()
        def prevCombStep = getPreviousCombinationStep()
        if (prevCombStep)
            result += prevCombStep.CumulativeValue()

        return result
    }

    int RestAmount(){
        def prevCombStep = getPreviousCombinationStep()
        if(prevCombStep == null){
            return combination.issueList.size()
        }else {
            if (prevCombStep.balancingStep.selectedCombination == combination)
                return prevCombStep.RestAmount() - 1
            else
                return prevCombStep.RestAmount()
        }
    }

    int Difference(){
        def prevCombStep = getPreviousCombinationStep()
        if(prevCombStep == null){
            return combination.evaluationOfProbabilities
        }else {
            if (prevCombStep.RestAmount() > 0)
                return JxQLN() - prevCombStep.CumulativeValue()
            else
                return MIN_VALUE
        }
    }
}
