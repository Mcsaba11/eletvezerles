# Életvezérlés (munkanév)

Natív Android-app (Kotlin + Jetpack Compose), amely egy helyen kezeli a napot: naptár, teendők,
hangulat, idővonal, és benne „appok”, elsőként a Satori függőségkezelő.

## Letöltés a telefonra

Minden feltöltés után a GitHub magától lefordítja az appot.

1. A tároló oldalán nyisd meg a **Releases** (Kiadások) részt, és válaszd a legfrissebbet.
2. Töltsd le az `eletvezerles-<szám>.apk` fájlt a telefonodon.
3. Nyisd meg, és engedélyezd a telepítést (ismeretlen forrásból, egyszer kell).

Az új verzió a régire települ, az adatok megmaradnak (minden build ugyanazzal a kulccsal van aláírva).

## Mi van benne (1. szakasz)

- **Ma nézet:** köszöntés, gyors bejegyzés, Satori-kártya (napok, szint, nap lezárása, SOS),
  hangulat és energia, mai naptár, esedékes teendők, napi gondolat.
- **Gyors bejegyzés:** egy mezőbe írsz („holnap 10-kor fogorvos”, „tankolás 18 000 Ft”,
  „meg kell hívni Pétert”, „hangulat 4”), az app szétválogatja. Internet nélkül működik.
- **Naptár:** havi nézet, saját események, és ha bekapcsolod, a telefon naptára (Google Naptár) is.
  Új eseményt a telefon naptárába is lehet menteni.
- **Teendők:** határidő, fontosság, csoportosítás (lejárt, ma, holnap, később, határidő nélkül).
- **Idővonal:** minden bejegyzés időrendben, szűrőkkel.
- **Satori:** a webes Satori natív újraépítése (Út, Eredmények, Napló, Mérleg, Mentor, SOS,
  vezetett légzés, testi idővonal, oklevelek, 5 téma, 3 szabályrendszer).
- **Beállítások:** ujjlenyomatos zár, esti emlékeztető (zárt appnál is), teljes mentés fájlba
  és visszaállítás, régi Satori-mentés betöltése.

## Felépítés

```
app/src/main/
  assets/content/hu/satori.json   a Satori összes szövege (angol változat: content/en/)
  res/values/strings.xml          a felület összes szövege (angol változat: values-en/)
  java/hu/lifeos/app/
    data/        adatbázis (Room), beállítások, telefon naptára, mentés
    capture/     gyorsbevitel-értelmező
    satori/      Satori: tartalom, állapot, szabályok, képernyők (ui/)
    ui/          közös képernyők és elemek
    notify/      napi emlékeztető
```

## Fordítás saját gépen

Android Studióban: File → Open → ez a mappa, majd Run. Parancssorból: `./gradlew assembleDebug`.
