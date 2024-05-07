package BalancingDemo1

@Grab('org.apache.poi:poi:3.8')
@Grab('org.apache.poi:poi-ooxml:3.8')
@GrabExclude('xml-apis:xml-apis')

String fileName = "SCM.22.R6.1_SCM.xlsx"
// new Balancing for the specified sprint
Balancing balancing = new Balancing(fileName)
// initialize
balancing.InitializeData(fileName)
// cartesian all of the combinations
balancing.Calculate()
balancing.SetAllCombination()
balancing.AssigneAllSteptoIssue()
balancing.divideIntoWeeks()




