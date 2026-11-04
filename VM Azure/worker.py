import time, json, sys, csv

VM_MIPS = 2600.0

def jalankan_tugas(id_tugas, panjang_mi):
    waktu_mulai = time.time()
    time.sleep(panjang_mi / VM_MIPS)
    waktu_selesai = time.time()
    hasil = {"id": id_tugas, "vm": sys.argv[1], "mulai": waktu_mulai, "selesai": waktu_selesai}
    print(json.dumps(hasil))

if __name__ == "__main__":
    nama_vm = sys.argv[1]
    file_csv = "/app/tugas.csv"

    with open(file_csv, mode='r') as file:
        reader = csv.DictReader(file)
        for row in reader:
            jalankan_tugas(row['id'], float(row['panjang_mi']))