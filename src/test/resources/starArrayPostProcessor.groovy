import com.blazemeter.jmeter.correlation.siebel.SiebelArrayFunction

String stringToSplit = ""
String rowId = ""

// Parsing Star Array parameter(s) using match number 1
stringToSplit = vars.get("Siebel_Star_Array_Op_1")
if (stringToSplit != null) {
    SiebelArrayFunction.split(stringToSplit, "Siebel_Star_Array_Op0", vars)
    rowIdValue = vars.get("Siebel_Star_Array_Op0_168")
    vars.put("Siebel_Star_Array_Op0_rowId", rowIdValue)
}

// Parsing Star Array parameter(s) using match number 2
stringToSplit = vars.get("Siebel_Star_Array_Op_2")
if (stringToSplit != null) {
    SiebelArrayFunction.split(stringToSplit, "Siebel_Star_Array_Op1", vars)
    rowIdValue = vars.get("Siebel_Star_Array_Op1_0")
    vars.put("Siebel_Star_Array_Op1_rowId", rowIdValue)
}

// Parsing Star Array parameter(s) using match number 3
stringToSplit = vars.get("Siebel_Star_Array_Op_3")
if (stringToSplit != null) {
    SiebelArrayFunction.split(stringToSplit, "Siebel_Star_Array_Op2", vars)
    rowIdValue = vars.get("Siebel_Star_Array_Op2_8")
    vars.put("Siebel_Star_Array_Op2_rowId", rowIdValue)
}

// Parsing Star Array parameter(s) using match number 4
stringToSplit = vars.get("Siebel_Star_Array_Op_4")
if (stringToSplit != null) {
    SiebelArrayFunction.split(stringToSplit, "Siebel_Star_Array_Op3", vars)
    rowIdValue = vars.get("Siebel_Star_Array_Op3_0")
    vars.put("Siebel_Star_Array_Op3_rowId", rowIdValue)
}

// Parsing Star Array parameter(s) using match number 5
stringToSplit = vars.get("Siebel_Star_Array_Op_5")
if (stringToSplit != null) {
    SiebelArrayFunction.split(stringToSplit, "Siebel_Star_Array_Op4", vars)
    rowIdValue = vars.get("Siebel_Star_Array_Op4_13")
    vars.put("Siebel_Star_Array_Op4_rowId", rowIdValue)
}

// Parsing Star Array parameter(s) using match number 6
stringToSplit = vars.get("Siebel_Star_Array_Op_6")
if (stringToSplit != null) {
    SiebelArrayFunction.split(stringToSplit, "Siebel_Star_Array_Op5", vars)
    rowIdValue = vars.get("Siebel_Star_Array_Op5_13")
    vars.put("Siebel_Star_Array_Op5_rowId", rowIdValue)
}

// Parsing Star Array parameter(s) using match number 7
stringToSplit = vars.get("Siebel_Star_Array_Op_7")
if (stringToSplit != null) {
    SiebelArrayFunction.split(stringToSplit, "Siebel_Star_Array_Op6", vars)
    rowIdValue = vars.get("Siebel_Star_Array_Op6_13")
    vars.put("Siebel_Star_Array_Op6_rowId", rowIdValue)
}

// Parsing Star Array parameter(s) using match number 8
stringToSplit = vars.get("Siebel_Star_Array_Op_8")
if (stringToSplit != null) {
    SiebelArrayFunction.split(stringToSplit, "Siebel_Star_Array_Op7", vars)
    rowIdValue = vars.get("Siebel_Star_Array_Op7_13")
    vars.put("Siebel_Star_Array_Op7_rowId", rowIdValue)
}

// Parsing Star Array parameter(s) using match number 9
stringToSplit = vars.get("Siebel_Star_Array_Op_9")
if (stringToSplit != null) {
    SiebelArrayFunction.split(stringToSplit, "Siebel_Star_Array_Op8", vars)
    rowIdValue = vars.get("Siebel_Star_Array_Op8_13")
    vars.put("Siebel_Star_Array_Op8_rowId", rowIdValue)
}

// Parsing Star Array parameter(s) using match number 10
stringToSplit = vars.get("Siebel_Star_Array_Op_10")
if (stringToSplit != null) {
    SiebelArrayFunction.split(stringToSplit, "Siebel_Star_Array_Op9", vars)
    rowIdValue = vars.get("Siebel_Star_Array_Op9_0")
    vars.put("Siebel_Star_Array_Op9_rowId", rowIdValue)
}