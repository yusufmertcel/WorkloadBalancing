package BalancingDemo1

import groovy.transform.ToString

@ToString(includeNames = true)
class PropertyDefinition {
    private String name
    List<PropValues> values

    PropertyDefinition(){
        values = []
    }

    List<PropValues> getValues(){
        return values
    }

    void AddValue(PropValues value){
        if( !values.contains(value) ) {
            values.add(value)
            value.property = this
        }
    }

    String toString(){
        name
    }
}
