package BalancingDemo1

import groovy.transform.ToString

@ToString
class Issue {
    private String key
    List<PropValues> propertyValues
    private Balancing balancing
    private Combination combination

    Issue(Balancing balancing){
        this.balancing = balancing
        propertyValues = new ArrayList<PropValues>()

    }

    void setCombination(Combination value){
        this.combination = value
        this.combination.AddValue(this)
    }

    BalancingStep getbalancingStep(){
        if(balancing == null)
            return null
        else
            return balancing.balancingSteps.find{ step ->
                step.assignedIssue == this
            }
    }

    String toString(){
        key
    }
}
