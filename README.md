# NeuroVoice

Ideja projekta je razvoj aplikacije za analizu ljudskog glasa, sa ciljem istraživanja karakteristika govora koje mogu biti povezane sa Parkinsonovom bolešću.

Parkinsonova bolest može uticati na govor, posebno na jačinu glasa, stabilnost frekvencije i pravilnost izgovora. Zbog toga je cilj da aplikacija obrađuje audio-snimke, izdvaja određene karakteristike glasa i kasnije ih koristi za dalju analizu i primenu mašinskog učenja.

Za analizu je odabrana baza podataka **Italian Parkinson's Voice and Speech**, koja sadrži stvarne audio-snimke zdravih osoba i osoba sa Parkinsonovom bolešću. Baza uključuje izgovaranje samoglasnika, ponavljanje slogova i čitanje tekstova.

Baza je dostupna na sledećem linku:

https://huggingface.co/datasets/birgermoell/Italian_Parkinsons_Voice_and_Speech

Projekat je započet implementacijom jednostavnih funkcija za obradu numeričkog signala. Nakon toga je kod proširen kako bi mogao da učitava stvarne WAV audio-snimke.

Trenutno su implementirane sledeće funkcije:

* **average** – računa prosečnu vrednost signala i omogućava proveru njegovog odstupanja od nule.
* **amplitude** – pronalazi maksimalnu apsolutnu amplitudu signala.
* **rms** – računa efektivnu vrednost signala, koja se koristi za procenu njegove jačine.
* **energy** – računa zbir kvadrata vrednosti signala i omogućava analizu energije određenog dela snimka.
* **peak-to-peak** – računa razliku između najveće i najmanje vrednosti signala.
* **analyze-signal** – objedinjuje prethodne funkcije i vraća rezultate analize.
* **read-sample** – pretvara dva bajta 16-bitnog PCM audio-zapisa u numeričku vrednost.
* **read-audio** – učitava WAV fajl, izdvaja njegove uzorke i frekvenciju uzorkovanja.

Za sada je program prilagođen za analizu dva snimka iz baze, jednog zdrave osobe i jednog osobe sa Parkinsonovom bolešću. Za svaki snimak izračunavaju se osnovne karakteristike audio-signala.

U daljem razvoju planirano je dodavanje novih funkcija koje bi omogućile detaljniju analizu glasa.

Prvo je potrebno omogućiti obradu većeg broja snimaka, njihovo deljenje na kraće segmente i normalizaciju podataka, kako bi rezultati mogli pravilnije da se porede.

Nakon toga planirana je implementacija funkcija za izdvajanje karakteristika koje su značajnije za istraživanje Parkinsonove bolesti.

Jedna od njih je **fundamental frequency (F0)**, odnosno osnovna frekvencija glasa. Na osnovu nje mogu se analizirati promene visine glasa.

Planirano je i izračunavanje **jitter-a**, koji meri nepravilnosti u trajanju uzastopnih glasovnih ciklusa, kao i **shimmer-a**, koji meri promene njihove amplitude.

Pored toga, trebalo bi dodati **HNR (Harmonics-to-Noise Ratio)**, koji predstavlja odnos harmonijskog dela signala i šuma, a kasnije i složenije karakteristike kao što je **PPE (Pitch Period Entropy)**.

Izbor ovih karakteristika zasniva se na istraživanju *Suitability of Dysphonia Measurements for Telemonitoring of Parkinson's Disease* (Little i saradnici, 2009), u kojem je analizirana njihova primena za razlikovanje glasova zdravih osoba i osoba sa Parkinsonovom bolešću.

Rad je dostupan na:

https://pmc.ncbi.nlm.nih.gov/articles/PMC3051371/


Kada budu implementirane potrebne funkcije za izdvajanje karakteristika, planirano je i istraživanje mogućnosti primene jednostavnog modela mašinskog učenja za prepoznavanje određenih obrazaca u govoru.

Za testiranje funkcija koristi se biblioteka Midje, a projekat se pokreće pomoću Leiningen-a komandom `lein run`.

Audio-snimci se čuvaju lokalno u direktorijumu `resources/audio/` i nisu uključeni u GitHub repozitorijum.

