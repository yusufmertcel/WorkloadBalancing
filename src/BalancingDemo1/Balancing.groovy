package BalancingDemo1

import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellReference

@SuppressWarnings
class Balancing {
    String sprintName
    List<NormalizedValues> normalizationValues
    List<PropertyDefinition> properties
    List<Combination> combinations
    List<Issue> issueList
    List<BalancingStep> balancingSteps
    ExcelWorkbook excel

    Balancing(String fileName) {
        normalizationValues = []
        properties = []
        combinations = []
        issueList = []
        balancingSteps = []
        excel = new ExcelWorkbook()
        sprintName = fileName.split("_SCM")[0]
        println(sprintName)
    }

    void InitializeData(String filename) {
        excel.OpenExcel(filename)
        ReadNorm()
        ReadProperties()
        CalculateCombinations()
        ReadIssueList()

        combinations.removeAll { comb ->
            comb.issueList.size() == 0
        }
        println("Comb:" + combinations.size())

        CalculateCombinationsProbabilities()
    }

    static int fib_index(int value){
        int f1=0, f2=1, tmp
        int i = 0 // 0 1 1 2 3 5 8 13
        while(f1 != value){
            tmp = f2
            f2 = f1 + f2
            f1 = tmp
            if(f1 != 1)
                i++
        }
        return i
    }

    int calcbusinessValue(int value){
        int threshold = value - (value % 100) + 50
        return value > threshold ? threshold + 50 : threshold - 50
    }


    void ReadNorm() {
        Sheet normData = null
        try{
            normData = excel.OpenSheet("PropertyValueNormalization")
        } catch(Exception e){
            println(e.message)
        }
        DataFormatter formatter = new DataFormatter()
        for (int index = 1; index <= normData.lastRowNum; index++) {
            Cell cell = normData.getRow(index).getCell(CellReference.convertColStringToIndex("A"))
            def currentNorm = new NormalizedValues()
            currentNorm.propertyName = formatter.formatCellValue(cell)
            cell = normData.getRow(index).getCell(CellReference.convertColStringToIndex("B"))
            currentNorm.originalValue = formatter.formatCellValue(cell)
            println("Original Value "+currentNorm.originalValue)
            cell = normData.getRow(index).getCell(CellReference.convertColStringToIndex("C"))
            if(currentNorm.originalValue.isInteger() && currentNorm.propertyName != 'Business Value'){
                 currentNorm.normalValue =  (char)( (int)'A' + (fib_index(Integer.valueOf(currentNorm.originalValue))) )
            }
            else if(currentNorm.propertyName == 'Business Value'){
                println(Double.valueOf(currentNorm.originalValue))
                currentNorm.normalValue = calcbusinessValue((int)Double.valueOf(currentNorm.originalValue))
            }
            else{
                currentNorm.normalValue = formatter.formatCellValue(cell)
            }
            normalizationValues.add(currentNorm)
            //println("$currentNorm.featureName | $currentNorm.originalValue | $currentNorm.normalValue")
        }
        println("Normalization Values: ${normalizationValues}")
    }

    void ReadProperties() {
        DataFormatter formatter = new DataFormatter()
        Sheet propData = null
        try{
            propData = excel.OpenSheet("PropertyDefinitions")
        } catch (Exception e){
            println(e.message)
        }
        boolean first = true
        for (Row row in propData) {
            if (first) {
                first = false
                continue
            }
            Cell cell = row.getCell(CellReference.convertColStringToIndex("A"))
            String propertyName = formatter.formatCellValue(cell)
            PropertyDefinition currentProperty = properties.find { property ->
                (propertyName == property.name)
            }
            if (!currentProperty) {
                currentProperty = new PropertyDefinition(name: propertyName)
                properties.add(currentProperty)
            }
            cell = row.getCell(CellReference.convertColStringToIndex("B"))
            String currentValue = formatter.formatCellValue(cell)
            NormalizedValues normalizedValue = normalizationValues.find { normalized ->
                normalized.propertyName == propertyName && normalized.originalValue == currentValue
            }
            if (normalizedValue) {
                currentValue = normalizedValue.normalValue
            }

            if (!currentProperty.values.any { pv -> pv.value == currentValue }) {
                def newPropertyValue = new PropValues(value: currentValue, property: currentProperty)
            }
            //println("$propertyName $currentValue")
            //println("$currentProperty.values.value $currentProperty.values.property")
        }
    }


    void ReadIssueList() {
        DataFormatter formatter = new DataFormatter()
        Sheet issueData = null
        try{
             issueData = excel.OpenSheet("Data4")
        } catch (Exception e){
            println(e.message)
        }
        int rowNr = 1
        Row row = issueData.getRow(rowNr)
        Cell cell = row.getCell(CellReference.convertColStringToIndex("A"))
        println("ROW NUMBER:"+issueData.getLastRowNum())
        while (rowNr <= issueData.getLastRowNum()) {
            String issueKey = formatter.formatCellValue(cell)
            Issue currentIssue = issueList.find { issue -> issue.key == issueKey }
            if(currentIssue == null) {
                currentIssue = new Issue(this)
                currentIssue.key = issueKey
                issueList.add(currentIssue)
            }else {
                throw new Exception("Listede aynı Key'e sahip farklı bir Issue var. Issue Key: ${issueKey}")
            }

            String colReference = "B"
            cell = row.getCell(CellReference.convertColStringToIndex(colReference))
            while (colReference <= CellReference.convertNumToColString(row.getLastCellNum())) {
                String issuePropertyValue = formatter.formatCellValue(cell)
                cell = issueData.getRow(0).getCell(CellReference.convertColStringToIndex(colReference))
                if (cell != null) {
                    String propertyName = formatter.formatCellValue(cell)
                    PropertyDefinition property = properties.find { pd -> pd.name == propertyName }
                    if (property != null) {
                        NormalizedValues propertyNormal = normalizationValues.find { nv ->
                            nv.propertyName == property.name && issuePropertyValue == nv.originalValue
                        }
                        if (propertyNormal != null) {
                            issuePropertyValue = propertyNormal.normalValue
                        }
                        PropValues propertyValue = property.values.find { pv ->
                            pv.value == issuePropertyValue
                        }
                        if (propertyValue != null)
                            currentIssue.propertyValues.add(propertyValue)
                    }
                }
                colReference = colReference.next()
                //println(colReference)
                cell = row.getCell(CellReference.convertColStringToIndex(colReference))
            }
            println("${currentIssue.key} ${currentIssue.propertyValues} ${currentIssue.propertyValues.property}")
            currentIssue.combination = GetCombination(currentIssue.propertyValues)
            rowNr++
            row = issueData.getRow(rowNr)
            cell = row?.getCell(CellReference.convertColStringToIndex("A"))
        }
    }

    void CalculateCombinations() {
        combinations = Combination.CalculateCombinations(properties)
        //println(combinations.size())
    }

    private void CalculateCombinationsProbabilities() {
        for (def comb in combinations) {
            comb.EvaluateProbabilities(issueList)
            //println("${comb.name} ${comb.evaluationOfProbabilities}")
        }
    }

    void Calculate() {
        balancingSteps.clear()

        while (issueList.size() > balancingSteps.size()) {
            BalancingStep newStep = new BalancingStep(this, balancingSteps ? balancingSteps.last() : null)
            balancingSteps.add(newStep)
        }
        //println(balancingSteps)
    }


    Combination GetCombination(List<PropValues> propertyValues) {
        List<Combination> filtered = new ArrayList<Combination>()
        filtered.addAll(this.combinations)

        for (PropValues pv in propertyValues) {
            filtered = filtered.findAll { comb -> comb.values.contains(pv) }
        }
        return filtered.find()
    }

    void SetAllCombination() {
        BalancingStep emptyStep = balancingSteps.find { bs -> bs.selectedCombination == null }
        while (emptyStep) {
            SetCombination(emptyStep.getMaxDiffCombination())
            emptyStep = balancingSteps.find { bs -> bs.selectedCombination == null }
        }
    }

    void SetCombination(Combination combination) {
        BalancingStep emptyStep = balancingSteps.find { bs -> bs.selectedCombination == null }
        if (emptyStep) {
            CombinationStepValues combinationStep = emptyStep.combinationSteps.find { cs ->
                cs.combination == combination
            }
            if (combinationStep.RestAmount() > 0) {
                emptyStep.selectedCombination = combination
            }
        }
    }

    void AssigneAllSteptoIssue() {
        BalancingStep unassignedStep = balancingSteps.find { bs -> bs.assignedIssue == null }

        while (unassignedStep != null) {
            Issue unassignedIssue = issueList.find { issue ->
                issue.getbalancingStep() == null && issue.combination == unassignedStep.selectedCombination
            }
            if (unassignedIssue != null) {
                unassignedStep.assignedIssue = unassignedIssue
            }
            //println("${unassignedStep.stepNumber} ${unassignedStep.selectedCombination} ${unassignedStep.assignedIssue}")
            unassignedStep = balancingSteps.find { bs -> bs.assignedIssue == null }
        }
    }

    List<Integer> BaseEstimate(List<Integer> spCoding, int base){
        spCoding = spCoding.each { sp->
            sp - base + 1
        }
        return spCoding
    }

    void divideIntoWeeks() {
        List<Integer> storyPointCoding = new ArrayList<Integer>()
        List<Integer> spSumsOfWeeks = new ArrayList<Integer>()
        List<BalancingStep> nextSprintSteps = new ArrayList<BalancingStep>()
        int sumCoding = 0
        // traverse issueList and find sum of Story Points (Coding)
        issueList.each { issue ->
            List<PropValues> propValues = issue.propertyValues.findAll { pv ->
                def pattern = ~/^Story Points \(Coding\)/
                def finder = pv.property.name =~ pattern
                finder.find()
            }
            //println("${issue} ${propValues.property} ${propValues}")
            String propStr = propValues[0] ? propValues[0] : "0"
            propStr = normalizationValues.find( {it.normalValue == propStr}).originalValue
            int spInt = Integer.valueOf(propStr)
            storyPointCoding.add(spInt)
        }
        int median = FindMedian(storyPointCoding.sort())
        println("Median ${median}")
        // Calculate avg sp value of 3 week
        int avgCoding = storyPointCoding.sum() / 3
        int deviationValue = -Math.sqrt(AvgDeviation(storyPointCoding.sort().asList()))
        println("Variance: $deviationValue")
        println("AvgCoding: " + avgCoding)

        // sequentially traverse balancingsteps and sum story points
        // if sumCoding becomes bigger than avgCoding pass to the next week
        int weekNumber = 3
        for (bs in balancingSteps.reverse()) {
            Issue stepIssue = bs.assignedIssue
            List<PropValues> propValues = stepIssue.propertyValues.findAll { pv ->
                def pattern = ~/^Story Points \(Coding\)/
                def finder = pv.property.name =~ pattern
                finder.find()
            }
            String propStr = propValues[0] ? propValues[0] : "0"
            propStr = normalizationValues.find( {it.normalValue == propStr}).originalValue
            int spValue = Integer.valueOf(propStr)

            if (sumCoding + spValue <= (avgCoding + deviationValue)) {
                bs.week = weekNumber
                sumCoding += spValue
            } else {
                deviationValue += (Math.abs(deviationValue)*Math.E)  // increase deviation value from the mean
                println(deviationValue)
                println("GirdiElse: ${sumCoding} $weekNumber")
                spSumsOfWeeks.add(sumCoding)
                if (weekNumber > 1)
                    weekNumber--
                bs.week = weekNumber
                sumCoding = spValue
            }
            println("${stepIssue} ${bs.week} ${spValue}")
        }
        if(spSumsOfWeeks.size() < 3 )
            spSumsOfWeeks.add(sumCoding)
        else
            spSumsOfWeeks[2] += sumCoding
        spSumsOfWeeks = spSumsOfWeeks.reverse()
        println("Week SP: ${spSumsOfWeeks}")
        // compare three weeks' story point sum values if this value is bigger than median
        // swap the week whose story point value is bigger than the other week with smaller week
        for (int i = 1; i < spSumsOfWeeks.size(); i++){
            int spDifference = Math.abs(spSumsOfWeeks.get(i) - spSumsOfWeeks.get(i-1))
            println("Difference: $spDifference")
            if (spDifference > median) {
                // bigger sum of SPs week
                List<BalancingStep> weekMoreSpSteps = balancingSteps.findAll { bs ->
                    bs.week == ((spSumsOfWeeks.get(i) > spSumsOfWeeks.get(i-1) ? i : i-1) + 1)
                }
                // smaller sum of SPs week
                List<BalancingStep> weekLessSpSteps = balancingSteps.findAll { bs ->
                    bs.week == ((spSumsOfWeeks.get(i) > spSumsOfWeeks.get(i-1) ? i-1 : i) + 1)
                }
                int smallValue, bigValue
                def MoreSpStep = weekMoreSpSteps.find { bs ->
                    def spValue = bs.assignedIssue.propertyValues.findAll { pv ->
                        def pattern = ~/^Story Points \(Coding\)/
                        def finder = pv.property.name =~ pattern
                        finder.find()
                    }
                    String propStr = spValue[0] ? spValue[0] : "0"
                    if(spValue[0] != null){
                        propStr = normalizationValues.find( {it.normalValue == propStr}).originalValue
                        bigValue = Integer.valueOf(propStr)
                    }
                    (bigValue >= (spDifference / 3) && bigValue <= spDifference)
                }
                def LessSpStep = weekLessSpSteps.find { bs ->
                    def spValue = bs.assignedIssue.propertyValues.findAll { pv ->
                        def pattern = ~/^Story Points \(Coding\)/
                        def finder = pv.property.name =~ pattern
                        finder.find()
                    }
                    String propStr = spValue[0] ? spValue[0] : "0"
                    if(spValue[0] != null){
                        propStr = normalizationValues.find( {it.normalValue == propStr}).originalValue
                        smallValue = Integer.valueOf(propStr)
                    }
                    (smallValue < spDifference / 3)
                }
                // update spSumsOfWeeks with new values
                if(MoreSpStep != null && LessSpStep != null){
                    def spSumsOfWeeksCopy = spSumsOfWeeks.clone()
                    spSumsOfWeeks[MoreSpStep.week - 1] += (smallValue - bigValue)
                    spSumsOfWeeks[LessSpStep.week - 1] += (bigValue - smallValue)
                    // if order changes do not change the issues
                    if(spSumsOfWeeks[i] > spSumsOfWeeks[i-1])
                        spSumsOfWeeks = spSumsOfWeeksCopy as List<Integer>
                    println(MoreSpStep.toString()+" "+LessSpStep.toString())
                    int moreStepIndex = balancingSteps.findIndexOf {bs -> bs.stepNumber == MoreSpStep.stepNumber}
                    int lessStepIndex = balancingSteps.findIndexOf {bs -> bs.stepNumber == LessSpStep.stepNumber}
                    // update the changed two issues' week
                    int tmp = balancingSteps.get(moreStepIndex).week
                    balancingSteps.get(moreStepIndex).week = balancingSteps.get(lessStepIndex).week
                    balancingSteps.get(lessStepIndex).week = tmp
                    balancingSteps.swap(moreStepIndex, lessStepIndex)
                    storyPointCoding.swap(moreStepIndex, lessStepIndex)
                    println(balancingSteps.get(moreStepIndex).toString()+" "+balancingSteps.get(lessStepIndex).toString())
                }
            }
        }
        // eğer herhangi iki hafta arasinda çok fark varsa yani avg valuedan mediandan daha fazla oranda uzaklaştıysa
        // spi fazla olan haftanın icindeki en kucuk spli issue ile spi az olan haftanın icindeki en buyuk spli issueyu
        // degis
        //nextSprintSteps = nextSprintSteps.reverse()
        //println(nextSprintSteps)
        println(balancingSteps.week)
        println(balancingSteps.size())
        println(storyPointCoding.size())
        println(spSumsOfWeeks)
        ExportIssueWeekTable(spSumsOfWeeks)
    }

    int AvgDeviation(List<Integer> storyPoint){
        int middle = (storyPoint.size() / 2)
        int mean = storyPoint.sum() / storyPoint.size()
        // if mean is bigger than median than there must be some too high values than the other values
        println("${FindMedian(storyPoint)} $mean")
        if(FindMedian(storyPoint) > mean){ // return median
            if(storyPoint.size() % 2 == 0){
                return (storyPoint.get(middle) + storyPoint.get(middle - 1)) / 2
            }else {
                return storyPoint.get(middle)
            }
        }else {// return median of up quarter
            int topQuarter = (middle + storyPoint.size()) / 2
            if(middle % 2 == 0){
                return (storyPoint.get(topQuarter) + storyPoint.get(topQuarter - 1)) / 2
            }else {
                return storyPoint.get(topQuarter)
            }
        }
    }

    int FindMedian(List<Integer> storyPoints){
        int size = storyPoints.size()
        int middle = size / 2
        if(size % 2 == 0){
            return (storyPoints.get(middle) + storyPoints.get(middle - 1)) / 2
        } else {
            return storyPoints.get(middle)
        }
    }

    void ExportIssueWeekTable(List<Integer> spWeeks){
        Sheet issueSheet
        if(excel.wb.getSheet("IssueWeekSheet"))
            excel.wb.removeSheetAt(excel.wb.getSheetIndex("IssueWeekSheet"))
        issueSheet = excel.wb.createSheet("IssueWeekSheet")
        int rowNr = 0
        List<String> rowHeaderValues = new ArrayList<String>()
        rowHeaderValues.add("#")
        rowHeaderValues.add("Selected Issue Type")
        rowHeaderValues.add("Key")
        rowHeaderValues.add("Week")
        rowHeaderValues.addAll(properties.sort {it.name}.name)
        println(properties.sort {it.name})
        Row row = issueSheet.createRow(rowNr)
        int colNr = 0
        for(def value in rowHeaderValues){
            Cell cell = row.createCell(colNr)
            cell.setCellValue(value)
            colNr++
        }
        List<String> rowValues = new ArrayList<String>()
        for(def step in balancingSteps) {
            println(step.assignedIssue.propertyValues.sort {it.property.name})
            rowNr++
            colNr = 0
            row = issueSheet.createRow(rowNr)
            rowValues.add(String.valueOf(step.stepNumber))
            rowValues.add(step.selectedCombination.name)
            rowValues.add(step.assignedIssue.key)
            rowValues.add(step.week)
            colNr += rowValues.size()
            def tempProp = step.assignedIssue.propertyValues.sort{it.property.name }
            while(colNr < rowHeaderValues.size()){
                String value = tempProp.find {pv -> pv.property.name == rowHeaderValues.get(colNr)}?.value
                if(value)
                    rowValues.putAt(colNr, value)
                else
                    rowValues.putAt(colNr, "-")
                colNr++
            }
            println(rowValues)
            Cell cell
            colNr = 0
            while(colNr < rowHeaderValues.size()){
                cell = row.createCell(colNr)
                cell.setCellValue(rowValues.get(colNr))
                colNr++
            }
            rowValues.clear()
        }
        def cell = issueSheet.getRow(0).createCell(colNr + 2)
        cell.setCellValue("Sum of Weeks' Story Point (Coding) Values")
        cell = issueSheet.getRow(0).createCell(colNr + 3)
        cell.setCellValue("Issue Counts Per Week")
        spWeeks.eachWithIndex {sum, index ->
            cell = issueSheet.getRow(index + 1).createCell(colNr + 1)
            cell.setCellValue(index + 1)
            cell = issueSheet.getRow(index + 1).createCell(colNr + 2)
            cell.setCellValue(sum)
            cell = issueSheet.getRow(index + 1).createCell(colNr + 3)
            cell.setCellValue(balancingSteps.count {it.week == index + 1})
        }
        excel.SaveExcel()
    }
}