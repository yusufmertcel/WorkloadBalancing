package BalancingDemo1

import groovy.transform.ToString

@ToString(includeNames = true)
class PropValues {
    private String value
    private PropertyDefinition property


    void setProperty(PropertyDefinition property) {
        if(this.property != property) {
            this.property = property
            this.property.AddValue(this)
        }
    }

    String toString(){
        value
    }
}
