param(
    [Parameter(Mandatory = $true)]
    [string]$ReadingsPath,
    [Parameter(Mandatory = $true)]
    [string]$OutputPath
)

$ErrorActionPreference = 'Stop'
$sourcePath = (Resolve-Path -LiteralPath $ReadingsPath).Path
$version = $null
$entries = [System.Collections.Generic.List[string]]::new()

foreach ($line in [System.IO.File]::ReadLines($sourcePath, [System.Text.Encoding]::UTF8)) {
    if ($null -eq $version -and $line -match '^# Unicode Version ([0-9.]+)$') {
        $version = $Matches[1]
    }
    if ($line.StartsWith('#') -or [string]::IsNullOrEmpty($line)) {
        continue
    }

    $fields = $line.Split([char]9, 3)
    if ($fields.Length -ne 3 -or $fields[1] -ne 'kMandarin') {
        continue
    }

    $sourceReading = $fields[2].Split(' ')[0]
    $reading = $sourceReading.ToLowerInvariant().Normalize([System.Text.NormalizationForm]::FormD)
    $plain = [System.Text.StringBuilder]::new()
    for ($index = 0; $index -lt $reading.Length; $index++) {
        $character = $reading[$index]
        if ($character -eq 'u' -and $index + 1 -lt $reading.Length -and [int]$reading[$index + 1] -eq 0x0308) {
            [void]$plain.Append('v')
            $index++
            continue
        }

        $category = [System.Globalization.CharUnicodeInfo]::GetUnicodeCategory($reading, $index)
        if ($category -in @(
                [System.Globalization.UnicodeCategory]::NonSpacingMark,
                [System.Globalization.UnicodeCategory]::SpacingCombiningMark,
                [System.Globalization.UnicodeCategory]::EnclosingMark
        )) {
            continue
        }
        if ($character -lt 'a' -or $character -gt 'z') {
            throw "Unsupported kMandarin reading '$($fields[2])' for $($fields[0])"
        }
        [void]$plain.Append($character)
    }

    $entries.Add(('{0}{1}{2}' -f $fields[0].Substring(2).ToUpperInvariant(), [char]9, $plain.ToString()))
}

if ($null -eq $version -or $entries.Count -eq 0) {
    throw 'The input does not appear to be a valid Unihan_Readings.txt file.'
}

$destination = [System.IO.Path]::GetFullPath($OutputPath)
[System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($destination)) | Out-Null
$writer = [System.IO.StreamWriter]::new($destination, $false, [System.Text.UTF8Encoding]::new($false))
try {
    $writer.NewLine = "`n"
    $writer.WriteLine("# Generated from Unicode $version Unihan_Readings.txt, property kMandarin.")
    $writer.WriteLine('# First reading selected per UAX #38 for zh-Hans when multiple readings exist.')
    $writer.WriteLine('# Format: uppercase hexadecimal code point, tab, lowercase tone-free ASCII pinyin (v represents u with diaeresis).')
    foreach ($entry in $entries) {
        $writer.WriteLine($entry)
    }
} finally {
    $writer.Dispose()
}

Write-Output "Wrote $($entries.Count) readings from Unicode $version to $destination"
