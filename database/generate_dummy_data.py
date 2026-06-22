#!/usr/bin/env python3
"""Generate data_dummy_300.sql for TelyuStay project."""

import random
from datetime import datetime, timedelta

random.seed(42)

GEDUNG = [
    ("GA", "Gedung Anggrek", "Putra"),
    ("GB", "Gedung Bougenville", "Putri"),
    ("GC", "Gedung Cendana", "Putra"),
    ("GD", "Gedung Dahlia", "Putri"),
    ("GE", "Gedung Eucalyptus", "Putra"),
    ("GF", "Gedung Flamboyan", "Putri"),
]

PRODI = [
    "Informatika", "Sistem Informasi", "Teknik Elektro", "Teknik Telekomunikasi",
    "Manajemen", "Ilmu Komunikasi", "Desain Komunikasi Visual", "Teknik Industri",
    "Akuntansi", "Teknik Logistik",
]

NAMA_DEPAN = [
    "Ahmad", "Budi", "Citra", "Dewi", "Eka", "Fajar", "Gita", "Hadi", "Indra", "Joko",
    "Kartika", "Lia", "Maya", "Nadia", "Omar", "Putri", "Qori", "Rizky", "Sinta", "Taufik",
    "Umar", "Vina", "Wulan", "Yoga", "Zahra", "Adit", "Bayu", "Candra", "Dian", "Erlang",
    "Farhan", "Galih", "Hana", "Irfan", "Jihan", "Kevin", "Laras", "Mirza", "Nabila", "Oki",
    "Prasetyo", "Qonita", "Rafi", "Salma", "Tegar", "Utami", "Vero", "Wahyu", "Xena", "Yusuf",
]

NAMA_BELAKANG = [
    "Pratama", "Wijaya", "Saputra", "Kusuma", "Hidayat", "Ramadhan", "Santoso", "Lestari",
    "Nugroho", "Permana", "Setiawan", "Anggraini", "Maulana", "Purnama", "Siregar", "Utami",
    "Firmansyah", "Gunawan", "Halim", "Iskandar", "Julianto", "Kurniawan", "Mahardika", "Nasution",
]

TUJUAN_IZIN = [
    "Pulang ke rumah orang tua", "Keperluan keluarga", "Acara wisuda saudara",
    "Kunjungan rumah sakit", "Urusan bank", "Keperluan akademik di kampus lain",
    "Mengikuti seminar", "Liburan akhir pekan", "Menghadiri pernikahan kerabat",
    "Urusan kependudukan", "Kunjungan ke kampus pusat", "Keperluan pribadi mendesak",
]

DESKRIPSI_LAPORAN = [
    "Lampu kamar mati", "Kran air bocor", "AC tidak dingin", "Pintu kamar susah dikunci",
    "Stop kontak rusak", "Wifi lemah di lantai ini", "Kamar mandi mampet", "Jendela tidak bisa ditutup",
    "Kipas angin berisik", "Lantai licin di koridor", "Keran shower rusak", "Tombol flush WC rusak",
    "Lampu koridor mati", "Kursi meja belajar patah", "Colokan listrik panas", "Ventilasi kamar tersumbat",
]

LOKASI_LAPORAN = [
    "Kamar", "Kamar mandi", "Koridor lantai", "Ruang tamu gedung", "Dapur bersama",
    "Musholla", "Laundry", "Parkir gedung", "Tangga darurat", "Ruang study",
]

AKTIVITAS = [
    "Login sistem", "Logout sistem", "Ajukan izin keluar", "Update profil",
    "Lapor fasilitas", "Bayar tagihan", "Cek status izin", "Cek status laporan",
    "Ubah data kamar", "Akses dashboard", "Chat asisten AI", "Export data",
]

IZIN_STATUS = ["DIAJUKAN", "DISETUJUI", "DITOLAK", "SELESAI"]
REPORT_STATUS = ["BARU", "DIPROSES", "SELESAI"]
PAYMENT_STATUS = ["BELUM_BAYAR", "LUNAS", "TERLAMBAT"]

NUM_MHS = 100
NUM_IZIN = 50
NUM_REPORT = 50
NUM_PAYMENT = 50
NUM_ACTIVITY = 50
# Total transactional: 300

def esc(s: str) -> str:
    return s.replace("'", "''")

def sql_val(v):
    if v is None:
        return "NULL"
    if isinstance(v, (int, float)):
        return str(v)
    return f"'{esc(str(v))}'"

def rand_phone():
    return "08" + str(random.randint(1000000000, 9999999999))

def rand_datetime(days_back=365):
    base = datetime.now() - timedelta(days=random.randint(0, days_back))
    base = base.replace(hour=random.randint(6, 22), minute=random.randint(0, 59), second=0)
    return base.strftime("%Y-%m-%d %H:%M:%S")

def rand_date(days_back=180):
    d = datetime.now().date() - timedelta(days=random.randint(0, days_back))
    return d.strftime("%Y-%m-%d")

def build_kamar():
    rows = []
    for kode, _, _ in GEDUNG:
        for lantai in range(1, 6):
            for nomor in range(1, 11):
                no = f"{kode}{lantai}{nomor:02d}"
                rows.append((no, 2, kode))
    return rows

def main():
    kamar_rows = build_kamar()  # 6 * 5 * 10 = 300 kamar - too many!
    # Reduce: 5 floors, 4 rooms per floor per building = 6*5*4 = 120 kamar
    kamar_rows = []
    for kode, _, _ in GEDUNG:
        for lantai in range(1, 6):
            for nomor in range(1, 5):
                no = f"{kode}{lantai}{nomor:02d}"
                kamar_rows.append((no, 2, kode))

    lines = []
    lines.append("-- ============================================================")
    lines.append("-- TelyuStay - Data Dummy 300 Record")
    lines.append("-- Import setelah database_asrama.sql")
    lines.append("-- Total: 100 mahasiswa + 50 izin + 50 laporan + 50 payment + 50 log = 300")
    lines.append("-- Plus data pendukung: gedung, kamar, senior resident, pengelola")
    lines.append("-- ============================================================")
    lines.append("")
    lines.append("USE database_asrama;")
    lines.append("")
    lines.append("-- Hapus data lama (urutan dari tabel anak ke induk, aman untuk phpMyAdmin)")
    lines.append("DELETE FROM activity_log;")
    lines.append("DELETE FROM payment;")
    lines.append("DELETE FROM facility_report;")
    lines.append("DELETE FROM izin_keluar;")
    lines.append("DELETE FROM users;")
    lines.append("DELETE FROM kamar;")
    lines.append("DELETE FROM gedung;")
    lines.append("")

    # Gedung
    lines.append("-- Gedung (6)")
    lines.append("INSERT INTO gedung (kode_gedung, nama_gedung, jenis_gedung) VALUES")
    gedung_vals = [f"({sql_val(k)}, {sql_val(n)}, {sql_val(j)})" for k, n, j in GEDUNG]
    lines.append(",\n".join(gedung_vals) + ";")
    lines.append("")

    # Kamar
    lines.append(f"-- Kamar ({len(kamar_rows)})")
    lines.append("INSERT INTO kamar (nomor_kamar, kapasitas, kode_gedung) VALUES")
    kamar_vals = [f"({sql_val(no)}, {kap}, {sql_val(kd)})" for no, kap, kd in kamar_rows]
    lines.append(",\n".join(kamar_vals) + ";")
    lines.append("")

    # Assign mahasiswa to rooms (max 2 per room)
    room_slots = []
    for no, kap, kd in kamar_rows:
        for _ in range(kap):
            room_slots.append((no, kd))
    random.shuffle(room_slots)
    room_slots = room_slots[:NUM_MHS]

    # Senior Resident & Pengelola
    lines.append("-- Senior Resident & Pengelola Gedung (12)")
    staff_rows = []
    for i, (kode, nama, _) in enumerate(GEDUNG, 1):
        staff_rows.append(
            f"({sql_val(f'SR{i:03d}')}, {sql_val(f'Senior Resident {nama}')}, 'SeniorResident', "
            f"{sql_val(f'sr_{kode.lower()}')}, '12345', {sql_val(f'SR-{i:02d}')}, NULL, NULL, NULL, NULL, {sql_val(kode)})"
        )
        staff_rows.append(
            f"({sql_val(f'PG{i:03d}')}, {sql_val(f'Pengelola {nama}')}, 'PengelolaGedung', "
            f"{sql_val(f'pg_{kode.lower()}')}, '12345', NULL, {sql_val(f'PG-{i:02d}')}, NULL, NULL, NULL, {sql_val(kode)})"
        )
    lines.append(
        "INSERT INTO users (id, nama, role, username, password, nomor_sr, nomor_pegawai, nim, prodi, no_telepon, kode_gedung_tugas) VALUES"
    )
    lines.append(",\n".join(staff_rows) + ";")
    lines.append("")

    # Mahasiswa
    mhs_ids = []
    lines.append(f"-- Mahasiswa ({NUM_MHS})")
    lines.append(
        "INSERT INTO users (id, nama, role, username, password, nim, prodi, no_telepon, nomor_kamar) VALUES"
    )
    mhs_vals = []
    used_usernames = set()
    for i in range(1, NUM_MHS + 1):
        mid = f"MHS{i:03d}"
        mhs_ids.append(mid)
        nama = f"{random.choice(NAMA_DEPAN)} {random.choice(NAMA_BELAKANG)}"
        base_user = nama.split()[0].lower()[:6] + str(i)
        username = base_user
        while username in used_usernames:
            username = base_user + str(random.randint(1, 99))
        used_usernames.add(username)
        nim = f"13012{random.randint(100000, 999999)}"
        prodi = random.choice(PRODI)
        phone = rand_phone()
        kamar, _ = room_slots[i - 1]
        mhs_vals.append(
            f"({sql_val(mid)}, {sql_val(nama)}, 'Mahasiswa', {sql_val(username)}, '12345', "
            f"{sql_val(nim)}, {sql_val(prodi)}, {sql_val(phone)}, {sql_val(kamar)})"
        )
    lines.append(",\n".join(mhs_vals) + ";")
    lines.append("")

    # Izin keluar
    lines.append(f"-- Izin Keluar ({NUM_IZIN})")
    lines.append(
        "INSERT INTO izin_keluar (izin_id, mahasiswa_id, tujuan, waktu_keluar, waktu_kembali, status, alasan_penolakan) VALUES"
    )
    izin_vals = []
    for i in range(1, NUM_IZIN + 1):
        mhs = random.choice(mhs_ids)
        status = random.choices(IZIN_STATUS, weights=[20, 35, 10, 35])[0]
        waktu_keluar = rand_datetime(120)
        waktu_kembali_dt = datetime.strptime(waktu_keluar, "%Y-%m-%d %H:%M:%S") + timedelta(
            hours=random.randint(2, 72)
        )
        waktu_kembali = waktu_kembali_dt.strftime("%Y-%m-%d %H:%M:%S")
        alasan = "Dokumen tidak lengkap" if status == "DITOLAK" else None
        izin_vals.append(
            f"({sql_val(f'IZN-{i:03d}')}, {sql_val(mhs)}, {sql_val(random.choice(TUJUAN_IZIN))}, "
            f"{sql_val(waktu_keluar)}, {sql_val(waktu_kembali)}, {sql_val(status)}, {sql_val(alasan)})"
        )
    lines.append(",\n".join(izin_vals) + ";")
    lines.append("")

    mhs_kamar = {mhs_ids[i]: room_slots[i][0] for i in range(NUM_MHS)}

    # Facility report
    lines.append(f"-- Laporan Fasilitas ({NUM_REPORT})")
    lines.append(
        "INSERT INTO facility_report (report_id, mahasiswa_id, deskripsi, lokasi, tanggal_lapor, status) VALUES"
    )
    report_vals = []
    for i in range(1, NUM_REPORT + 1):
        mhs = random.choice(mhs_ids)
        kamar_no = mhs_kamar[mhs]
        lokasi = f"{random.choice(LOKASI_LAPORAN)} {kamar_no[-3:]}"
        report_vals.append(
            f"({sql_val(f'RPT-{i:03d}')}, {sql_val(mhs)}, {sql_val(random.choice(DESKRIPSI_LAPORAN))}, "
            f"{sql_val(lokasi)}, {sql_val(rand_datetime(90))}, {sql_val(random.choice(REPORT_STATUS))})"
        )
    lines.append(",\n".join(report_vals) + ";")
    lines.append("")

    # Payment
    lines.append(f"-- Pembayaran ({NUM_PAYMENT})")
    lines.append(
        "INSERT INTO payment (payment_id, mahasiswa_id, jumlah_tagihan, denda, tanggal_bayar, status) VALUES"
    )
    payment_vals = []
    for i in range(1, NUM_PAYMENT + 1):
        mhs = random.choice(mhs_ids)
        status = random.choices(PAYMENT_STATUS, weights=[30, 55, 15])[0]
        tagihan = random.choice([750000, 850000, 900000, 1000000, 1200000])
        denda = random.choice([0, 0, 0, 50000, 100000, 150000]) if status != "LUNAS" else 0
        tgl_bayar = rand_date(60) if status == "LUNAS" else None
        payment_vals.append(
            f"({sql_val(f'PAY-{i:03d}')}, {sql_val(mhs)}, {tagihan}, {denda}, {sql_val(tgl_bayar)}, {sql_val(status)})"
        )
    lines.append(",\n".join(payment_vals) + ";")
    lines.append("")

    # Activity log
    lines.append(f"-- Activity Log ({NUM_ACTIVITY})")
    lines.append(
        "INSERT INTO activity_log (log_id, user_id, aktivitas, keterangan, waktu_aktivitas) VALUES"
    )
    all_user_ids = mhs_ids + [f"SR{i:03d}" for i in range(1, 7)] + [f"PG{i:03d}" for i in range(1, 7)]
    activity_vals = []
    for i in range(1, NUM_ACTIVITY + 1):
        uid = random.choice(all_user_ids)
        akt = random.choice(AKTIVITAS)
        ket = f"Aktivitas oleh {uid} pada sistem TelyuStay"
        activity_vals.append(
            f"({sql_val(f'LOG-{i:03d}')}, {sql_val(uid)}, {sql_val(akt)}, {sql_val(ket)}, {sql_val(rand_datetime(30))})"
        )
    lines.append(",\n".join(activity_vals) + ";")
    lines.append("")

    total = (
        len(GEDUNG) + len(kamar_rows) + 12 + NUM_MHS
        + NUM_IZIN + NUM_REPORT + NUM_PAYMENT + NUM_ACTIVITY
    )
    lines.append(f"-- TOTAL RECORDS: {total} (termasuk data pendukung)")
    lines.append(f"-- Data transaksional inti: {NUM_MHS + NUM_IZIN + NUM_REPORT + NUM_PAYMENT + NUM_ACTIVITY}")

    out = "d:\\apps\\folder tugas\\PBO\\tubes_pbo_rasentyur\\data_dummy_300.sql"
    with open(out, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print(f"Generated {out}")
    print(f"Total records: {total}")

if __name__ == "__main__":
    main()
