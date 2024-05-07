package BalancingDemo1

import org.apache.ivy.core.module.status.Status
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory

class ExcelWorkbook {
    Workbook wb
    void OpenExcel(String filename) {
        try {
            wb = WorkbookFactory.create(new FileInputStream(filename))
        } catch (Exception e) {
            println(e.message)
        }
    }

    Sheet OpenSheet(String sheetName) {
        Sheet data = null
        try{
            data = wb.getSheet(sheetName)
            if(!data){
                throw new NullPointerException("Boyle bir Excel Sheet'i yoktur.")
            }
        }catch (Exception e) {
            println(e.message)
        }
        return data
    }

    void SaveExcel(){
        try (OutputStream fileOut = new FileOutputStream("SCM.22.R6.1_SCM.xlsx")) {
            wb.write(fileOut);
        } catch (FileNotFoundException e){
            println(e.message)
        }
        println("Dosya kaydedilmistir.")
    }
}
