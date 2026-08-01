param (
    [Parameter(Mandatory=$true)]
    [string]$RepoUrl
)

Write-Host "Configuring GitHub remote repository..." -ForegroundColor Cyan
git remote remove origin 2>$null
git remote add origin $RepoUrl
git branch -M main

Write-Host "Pushing project to GitHub ($RepoUrl)..." -ForegroundColor Yellow
git push -u origin main

if ($LASTEXITCODE -eq 0) {
    Write-Host "[SUCCESS] Project pushed successfully to GitHub!" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Failed to push to GitHub. Please verify your repository URL and authentication." -ForegroundColor Red
}
