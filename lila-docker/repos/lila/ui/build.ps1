#!/usr/bin/env pwsh

$ArgsArray = @()
foreach ($arg in $args) {
    $ArgsArray += $arg
}

node "$PSScriptRoot/build.mjs" $ArgsArray
