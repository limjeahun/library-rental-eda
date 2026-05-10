param(
    [string] $Root = (Get-Location).Path,
    [switch] $Json,
    [switch] $FailOnFinding
)

$ErrorActionPreference = "Stop"

$resolvedRoot = (Resolve-Path -LiteralPath $Root).Path
$moduleNames = @(
    "book-service",
    "member-service",
    "rental-service",
    "bestbook-service",
    "common-events"
)

$sourceRoots = foreach ($moduleName in $moduleNames) {
    $modulePath = Join-Path $resolvedRoot $moduleName
    $mainPath = Join-Path $modulePath "src/main/java"
    if (Test-Path -LiteralPath $mainPath) {
        $mainPath
    }
}

$findings = New-Object System.Collections.Generic.List[object]

function Convert-ToRelativePath {
    param([string] $Path)

    if ($Path.StartsWith($resolvedRoot)) {
        return $Path.Substring($resolvedRoot.Length).TrimStart([char[]] @("\", "/"))
    }

    return $Path
}

function Add-Finding {
    param(
        [string] $Severity,
        [string] $Rule,
        [string] $File,
        [int] $Line,
        [string] $Text
    )

    $findings.Add([pscustomobject] @{
        Severity = $Severity
        Rule = $Rule
        File = Convert-ToRelativePath -Path $File
        Line = $Line
        Text = $Text.Trim()
    }) | Out-Null
}

function Get-JavaFiles {
    param([string[]] $Paths)

    foreach ($path in $Paths) {
        if (Test-Path -LiteralPath $path) {
            Get-ChildItem -LiteralPath $path -Recurse -File -Filter "*.java"
        }
    }
}

function Search-InFiles {
    param(
        [object[]] $Files,
        [string] $Severity,
        [string] $Rule,
        [string] $Pattern
    )

    foreach ($file in $Files) {
        Select-String -LiteralPath $file.FullName -Pattern $Pattern | ForEach-Object {
            Add-Finding -Severity $Severity -Rule $Rule -File $_.Path -Line $_.LineNumber -Text $_.Line
        }
    }
}

function Where-PathMatches {
    param(
        [object[]] $Files,
        [string] $Pattern
    )

    $Files | Where-Object {
        ($_.FullName -replace "\\", "/") -match $Pattern
    }
}

$allJavaFiles = @(Get-JavaFiles -Paths $sourceRoots)
$domainFiles = @(Where-PathMatches -Files $allJavaFiles -Pattern "/src/main/java/.*/domain/")
$applicationFiles = @(Where-PathMatches -Files $allJavaFiles -Pattern "/src/main/java/.*/application/")
$webRequestFiles = @(
    Where-PathMatches -Files $allJavaFiles -Pattern "/adapter/in/web/" |
        Where-Object { $_.Name -like "*Request.java" }
)
$applicationCommandFiles = @(
    Where-PathMatches -Files $allJavaFiles -Pattern "/application/dto/" |
        Where-Object { $_.Name -like "*Command.java" }
)
$applicationServiceFiles = @(Where-PathMatches -Files $allJavaFiles -Pattern "/application/service/")
$commonEventFiles = @(Where-PathMatches -Files $allJavaFiles -Pattern "/common-events/src/main/java/")

Search-InFiles -Files $allJavaFiles -Severity "P0" -Rule "direct-service-http-client" -Pattern "RestTemplate|WebClient|OpenFeign|@FeignClient"
Search-InFiles -Files $allJavaFiles -Severity "P0" -Rule "common-events-domain-vo-usage" -Pattern "com\.example\.library\.common\.vo|common[./\\]vo|new\s+IDName\b|new\s+Item\b"
Search-InFiles -Files $domainFiles -Severity "P0" -Rule "domain-outward-dependency" -Pattern "^import\s+(org\.springframework|jakarta\.persistence|javax\.persistence|org\.apache\.kafka|org\.springframework\.data|com\.example\.library\.common\.event|.*\.adapter\.|.*\.config\.)"
Search-InFiles -Files $applicationFiles -Severity "P0" -Rule "application-outward-dependency" -Pattern "^import\s+.*(\.adapter\.|\.config\.|org\.springframework\.web|org\.springframework\.kafka|jakarta\.persistence|javax\.persistence|org\.springframework\.data)"
Search-InFiles -Files $webRequestFiles -Severity "P1" -Rule "web-request-domain-boundary" -Pattern "toIdName\(|toItem\(|toRentalMember\(|toRentalItem\(|toDomainVo\(|^import\s+.*\.domain\."
Search-InFiles -Files $applicationCommandFiles -Severity "P1" -Rule "application-command-domain-boundary" -Pattern "^import\s+.*\.domain\."
Search-InFiles -Files $applicationServiceFiles -Severity "P1" -Rule "application-service-class-constant" -Pattern "^\s*(private|public|protected)?\s*static\s+final\s+"
Search-InFiles -Files $commonEventFiles -Severity "P1" -Rule "common-event-service-specific-conversion" -Pattern "\bto(Rental|Member|Book|BestBook|Domain|Command)\b|fromRequest\("

$commonVoPath = Join-Path $resolvedRoot "common-events/src/main/java/com/example/library/common/vo"
if (Test-Path -LiteralPath $commonVoPath) {
    Add-Finding -Severity "P0" -Rule "common-events-common-vo-package" -File $commonVoPath -Line 1 -Text "common-events/common/vo package exists"
}

$orderedFindings = @($findings | Sort-Object Severity, Rule, File, Line)

if ($Json) {
    ConvertTo-Json -InputObject $orderedFindings -Depth 4
} elseif ($orderedFindings.Count -eq 0) {
    Write-Output "No architecture scan findings."
} else {
    foreach ($finding in $orderedFindings) {
        Write-Output ("[{0}] {1} {2}:{3}" -f $finding.Severity, $finding.Rule, $finding.File, $finding.Line)
        Write-Output ("    {0}" -f $finding.Text)
    }

    Write-Output ""
    Write-Output ("Findings: {0}" -f $orderedFindings.Count)
    Write-Output "Treat these as review leads. Confirm each finding against AGENTS.md before changing code."
}

if ($FailOnFinding -and $orderedFindings.Count -gt 0) {
    exit 2
}
