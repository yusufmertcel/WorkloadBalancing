package BalancingDemo1

class Combination {
    private String name
    private List<PropValues> values
    private List<Issue> issueList
    private int evaluationOfProbabilities

    Combination(){
        values = []
        issueList = []
    }

    void AddValue(Issue value){
        if(!issueList.contains(value)){
            issueList.add(value)
            value.combination = this
        }
    }

    static List<Combination> CalculateCombinations(List<PropertyDefinition> properties){
        Next(properties, 0, new ArrayList<PropValues>())
    }

    private static Next(List<PropertyDefinition> properties, int propIndex, List<PropValues> values){
        List<Combination> result = new ArrayList<Combination>()

        PropertyDefinition currentProp = properties[propIndex]
        //println(currentProp.values)
        for(PropValues crVal : currentProp?.values){
            //println("Girdi "+properties.size()+ " " + propIndex)
            //println(crVal.value)
            if(propIndex < properties.size() - 1){
                values.add(crVal)
                result.addAll(Next(properties, propIndex + 1, values))
                values.remove(crVal)
            }
            else {
                Combination current = new Combination()
                current.values.addAll(values)
                current.values.add(crVal)
                //println("Girdi2")
                current.name = current.values.collect {pv -> pv.value}.join(".")
                //println(current)
                result.add(current)
            }
        }
        return result
    }

    void EvaluateProbabilities(List<Issue> allIssues) {
        evaluationOfProbabilities = 0
        for(def val in values){
            evaluationOfProbabilities += allIssues.count {issue ->
                issue.propertyValues.contains(val)
            }
        }
    }

    String toString(){
        this.name
    }
}
