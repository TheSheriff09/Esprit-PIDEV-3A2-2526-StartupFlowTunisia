Add-Type -AssemblyName System.Speech

$engine = New-Object System.Speech.Recognition.SpeechRecognitionEngine
$grammar = New-Object System.Speech.Recognition.DictationGrammar
$engine.LoadGrammar($grammar)
$engine.SetInputToDefaultAudioDevice()

# Short 3-second chunks so text appears in real-time while user is speaking
while ($true) {
    try {
        $result = $engine.Recognize([System.TimeSpan]::FromSeconds(3))
        if ($null -ne $result -and $result.Text -ne '') {
            Write-Host $result.Text
            [Console]::Out.Flush()
        }
    }
    catch {
        break
    }
}
