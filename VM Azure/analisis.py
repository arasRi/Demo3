import json

def hitung_makespan(file_log):
    waktu_mulai_awal = float('inf')
    waktu_selesai_akhir = 0

    with open(file_log, 'r') as f:
        for line in f:
            try:
                data = json.loads(line)
                if data['mulai'] < waktu_mulai_awal:
                    waktu_mulai_awal = data['mulai']
                if data['selesai'] > waktu_selesai_akhir:
                    waktu_selesai_akhir = data['selesai']
            except json.JSONDecodeError:
                continue # Abaikan baris teks biasa

    return waktu_selesai_akhir - waktu_mulai_awal

m0 = hitung_makespan('log_vm0.json')
m1 = hitung_makespan('log_vm1.json')
m2 = hitung_makespan('log_vm2.json')

print(f"Makespan VM0: {m0:.2f} detik")
print(f"Makespan VM1: {m1:.2f} detik")
print(f"Makespan VM2: {m2:.2f} detik")