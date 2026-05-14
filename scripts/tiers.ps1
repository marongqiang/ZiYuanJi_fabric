$dataDir = 'src/main/resources/data/chickens/chickens'
$types = @{}
Get-ChildItem $dataDir -Filter *.json | ForEach-Object {
    $json = Get-Content $_.FullName -Raw | ConvertFrom-Json
    $cid = $_.BaseName
    $types[$cid] = @{
        p1 = if ($json.parent1) { $json.parent1 -replace 'chickens:', '' } else { $null }
        p2 = if ($json.parent2) { $json.parent2 -replace 'chickens:', '' } else { $null }
        spawn = if ($json.spawn_type) { $json.spawn_type } else { 'NONE' }
        lay = $json.lay_item
    }
}

$tiers = @{}
function Get-Tier($cid, $visiting) {
    if ($tiers.ContainsKey($cid)) { return $tiers[$cid] }
    if (-not $types.ContainsKey($cid)) { $tiers[$cid] = 1; return 1 }
    $t = $types[$cid]
    if ($null -eq $t.p1 -or $null -eq $t.p2) { $tiers[$cid] = 1; return 1 }
    if ($visiting -contains $cid) { $tiers[$cid] = 1; return 1 }
    $v2 = $visiting + $cid
    $t1 = Get-Tier $t.p1 $v2
    $t2 = Get-Tier $t.p2 $v2
    $tier = 1 + [Math]::Max($t1, $t2)
    $tiers[$cid] = $tier
    return $tier
}

foreach ($cid in $types.Keys) { Get-Tier $cid @() }

$byTier = $tiers.GetEnumerator() | Group-Object Value | Sort-Object Name
foreach ($group in $byTier) {
    $tier = $group.Name
    $list = $group.Group | Sort-Object Name
    Write-Host "`n=== Tier $tier ($($list.Count)) ==="
    foreach ($item in $list) {
        $cid = $item.Name
        $t = $types[$cid]
        $ways = @()
        if ($null -eq $t.p1 -and $null -eq $t.p2) {
            if ($cid -in 'red','white','blue','yellow','green','black','snowball') { $ways += 'Teach(dye 10x)' }
            elseif ($cid -match 'log|stem') { $ways += 'Teach(log 10x)' }
            elseif ($cid -eq 'water') { $ways += 'Teach(bottle 3x)' }
            elseif ($cid -eq 'smart') { $ways += 'Book->vanilla' }
            elseif ($cid -eq 'stone') { $ways += 'Cobble egg->furnace' }
            elseif ($cid -eq 'smooth_stone') { $ways += 'Stone egg->furnace' }
        }
        if ($t.spawn -eq 'NORMAL') { $ways += 'Spawn(Grass)' }
        elseif ($t.spawn -eq 'HELL') { $ways += 'Spawn(Nether)' }
        if ($t.p1 -and $t.p2) { $ways += "Breed($($t.p1)+$($t.p2))" }
        $waysStr = $ways -join ' | '
        Write-Host "  $cid -> $($t.lay)  [$waysStr]"
    }
}
