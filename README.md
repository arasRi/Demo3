Batasan Demo Dunia Nyata:
1.Hanya membahas 1 Data Center
2.Menggunakan Standard B2ats v2 (2 vcpus, 1 GiB memory) untuk VM Azure

Setup Azure:
1.Login azure dan buat VM Azure
2.Masuk ke vm dan buat/copy persiapan [Dockerfile, worker.py, analis.py, tugas.csv, tugas_vm1.csv, tugas_vm2.csv, tugas_vm3.csv]
3.Buat image docker image dan container vm0, vm1, vm2
```
sudo docker build -t worker-image .

sudo docker run -d --name vm0 --cpus="0.5" -v $(pwd)/tugas_vm0.csv:/app/tugas.csv worker-image python worker.py VM0 tugas.csv &
sudo docker run -d --name vm1 --cpus="0.5" -v $(pwd)/tugas_vm1.csv:/app/tugas.csv worker-image python worker.py VM1 tugas.csv &
sudo docker run -d --name vm2 --cpus="1.0" -v $(pwd)/tugas_vm2.csv:/app/tugas.csv worker-image python worker.py VM2 tugas.csv &
```
4.Log hasil dan gunakan analisis.py
```
sudo docker logs vm0 > log_vm0.json &
sudo docker logs vm1 > log_vm1.json &
sudo docker logs vm2 > log_vm2.json &

python3 analis.py
```

Setup CloudSim-example:
1. Download CloudSim-examples-master dan CloudSim-master
2. Copy file [tugas.csv, HillClimbingExp.java]

| VM   | Azure | Cloudsim | Beda (%) |
| ---- | ----- | -------- | -------- |
| 0    | 31.50 | 33.39    | +6.00%   |
| 1    | 32.97 | 12.62    | -61.72%  |
| 2    | 36.36 | 39.92    | +9.79%   |

- Ada perbedaan waktu karena influensi dunia nyata

- Waktu drastis di cloudsim VM1 karena Noisy Neighboor

- Waktu mirip karena sudah di konversi di VM Azure


`Beda (%) = ((CloudSim - VM Azure) / VM Azure) * 100`
