# NeuroVoice

Ideja projekta je razvoj aplikacije za analizu ljudskog glasa, sa ciljem istraživanja karakteristika govora koje mogu biti povezane sa Parkinsonovom bolešću.

Parkinsonova bolest može uticati na govor, na primer na jačinu glasa, njegovu stabilnost i promene u frekvenciji. Zbog toga je ideja da aplikacija obrađuje audio-snimke i izdvaja određene karakteristike koje bi kasnije mogle da se koriste za dalju analizu i primenu mašinskog učenja.

Za početak je korišćen jednostavan signal:

`[0.2 0.5 -0.3 0.7 -0.1]`

Na njemu se testiraju sledeće funkcije:

* **average** – računa prosečnu vrednost signala. Koristi se za proveru da li je signal u proseku pomeren u odnosu na nulu.
* **amplitude** – pronalazi najveću apsolutnu vrednost signala i omogućava određivanje najvećeg odstupanja signala od nule.
* **rms** – računa efektivnu vrednost signala koja se koristi za procenu njegove jačine.
* **energy** – računa zbir kvadrata vrednosti signala. Kasnije se može koristiti za praćenje energije različitih delova govora.
* **peak-to-peak** – računa razliku između najveće i najmanje vrednosti signala i pokazuje njegov ukupni amplitudski raspon.
* **analyze-signal** – objedinjuje prethodne funkcije i vraća rezultate analize u obliku mape.

Trenutno se analiza vrši nad ručno unetim vrednostima, a plan je da se projekat postepeno nadograđuje. Sledeći korak je omogućavanje učitavanja stvarnih audio-snimaka i njihove obrade. Nakon toga planirano je izdvajanje dodatnih karakteristika glasa, posebno onih koje se koriste u istraživanjima Parkinsonove bolesti.

U kasnijoj fazi planirana je i primena jednostavnog modela mašinskog učenja koji bi na osnovu izdvojenih karakteristika pokušao da prepozna određene obrasce u govoru.

Cilj projekta nije postavljanje medicinske dijagnoze, već istraživanje mogućnosti analize glasa pomoću programskog jezika Clojure.

Projekat je napravljen korišćenjem Leiningen-a i pokreće se komandom `lein run`.

