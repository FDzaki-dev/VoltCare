# CHANGELOG.md
(Urutan DESCENDING - entri terbaru di paling atas)

## [v1.0.0-batch1] - 2026-08-19
### Added
- Initial project setup: Kotlin + Compose + Room + Foreground Service architecture.
- 4-tab navigation: Dashboard, Riwayat, Penguras, Aturan.
- Dashboard fungsional (Health%, Suhu, Volt, Cas, Estimasi, Cycle, tombol Mulai Kalibrasi).
- BatteryMonitorService: sampling, logging, retensi 30 hari, evaluasi rule dasar.
- Crash logger bawaan (MediaStore, FIFO 50 log).
- GitHub Actions release.yml dengan Stale Run Guard + GitHub Release publish.
- Auto-generated release keystore (RSA 2048) + secrets export file.
### Deferred (lihat PROJECT_STATE.md > Pending Queue)
- Kalibrasi engine, Cycle Counter presisi, Drain Analyzer, Riwayat grafik + CSV,
  Stress Test, Rules UI editor.
