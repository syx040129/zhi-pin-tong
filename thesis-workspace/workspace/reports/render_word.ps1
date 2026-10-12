$ErrorActionPreference = 'Stop'
$reportPath = 'G:\project\智聘通招聘网站的设计与实现_开题报告.docx'
$pdfPath = 'G:\project\CareerNova\thesis-workspace\workspace\reports\render\proposal.pdf'
New-Item -ItemType Directory -Path (Split-Path -Parent $pdfPath) -Force | Out-Null
$wordApp = New-Object -ComObject Word.Application
$wordApp.Visible = $false
$wordApp.DisplayAlerts = 0
try {
    $reportDocument = $wordApp.Documents.Open($reportPath, $false, $true)
    $reportDocument.Repaginate()
    $reportDocument.ExportAsFixedFormat($pdfPath, 17)
    Write-Output "Pages: $($reportDocument.ComputeStatistics(2))"
    $reportDocument.Close(0)
} finally {
    $wordApp.Quit()
    [System.Runtime.InteropServices.Marshal]::ReleaseComObject($wordApp) | Out-Null
}
