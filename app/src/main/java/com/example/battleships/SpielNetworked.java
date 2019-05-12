package com.example.battleships;

import java.util.Random;

/**
 *
 * Diese Klasse implementiert die Spiellogik.
 *
 * Der Spielablauf soll in der HauptActivity implementiert werden.
 *
 * Created by InfoSphere on 30.08.2017.
 */

public class SpielNetworked {

    // Konstanten für Feldzustände
    public static final int WASSER = 0;
    public static final int SCHIFF = 1;
    public static final int WASSERTREFFER = 2;
    public static final int SCHIFFTREFFER = 3;

    // Konstanten für Spieler
    public static final int COMPUTER = 1;
    public static final int MENSCH = 0;
    public static final int NIEMAND = -1;

    // Konstanten für Spielzustände
    public static final int SPIELNEUSTART = 0;
    public static final int SPIELERSCHIESST = 1;
    public static final int SPIELERVERFEHLT = 2;
    public static final int COMPUTERSCHIESST = 3;
    public static final int COMPUTERVERFEHLT = 4;
    public static final int SPIELERGEWINNT = 5;
    public static final int SPIELERVERLIERT = 6;

    // Parameter des Spiels
    private final int MAX_LAENGE;
    private final int MIN_LAENGE;
    private final int FELDGROESSE;

    // Spielzustand
    private int spielzustand = 0;
    private int aktiverSpieler = MENSCH;

    private Random rnd;

    private int[][][] feld;

    /* speichert die Anzahl der Schiffe beider Spieler nach Länge
     *
     * Schiffe der Länge "0" entsprechen nicht erfolgreich gesetzen Zufallsschiffen
     * (siehe KI.platziereZufallsSchiff(int,int))
     *
     * schiffe[laenge][spieler]
     */
    private int[][] schiffe;

    // KI für Computergegner und Schiffsplatzierung
    private KI connectionManager;

    private int[] spielerziel = new int[2];
    private boolean zielGesetzt = false;
    private int[] computerziel = new int[2];

    /*
     * Standardkonstruktor mit Spielparametern, die nach Arbeitsblatt vorgesehen sind.
     *
     * Füllt das SpielActivity-Array und platziert Schiffe.
     */
    public SpielNetworked(long seed) {
        this(8,4,2, seed);
    }

    /*
     * Individualisierbarer Konstruktor
     *
     * Das Spielfeld ist immer quadratisch.
     *
     * Die Anzahl der Schiffe ist immer von der Länge abhängig. Ein Schiffe der Maximallänge wird einmal platziert. Das nächstekürzere zweimal, usw. bis zur Minimallänge.
     *
     * Die Schiffe werden zufällig von der Klasse KI platziert.
     *
     * feldGroesse: Höhe und Breite des Spielfelds
     * max_laenge, min_laenge: Grenzen für Länge der Schiffe
     *
     */
    public SpielNetworked(int feldGroesse, int max_laenge, int min_laenge, long seed) {
        this.FELDGROESSE = feldGroesse;
        this.MAX_LAENGE = max_laenge;
        this.MIN_LAENGE = min_laenge;
        this.rnd = new Random(seed);
        initialize();
    }

    // ******************************** getter & setter ********************************


    /*
     *  Gibt ein einzelnes Feld zurück
     *
     *  @return:
     *  -1: Fehler
     *  sonst: siehe Parameter
     *
     */
    public int getFeld(int x, int y, int spieler) {
        // Wenn nicht OutOfBounds, gebe Feldzustand zurück
        if (istImFeld(x,y))
            if ((spieler >= 0) && (spieler < feld[x][y].length))
                return feld[x][y][spieler];

        // sonst, gebe Fehlerwert zurück
        return -1;
    }

    /*
     *  Setzt ein einzelnes Feld auf einen bestimmten Wert.
     *
     *  Diese Methode sollte eigentlich nicht benötigt werden.
     */
    public void setFeld(int x, int y, int spieler, int wert) {
        // Wenn nicht OutOfBounds, gebe Feldzustand zurück
        if (istImFeld(x,y))
                if ((spieler >=0) && (spieler < feld[x][y].length))
                    if ((wert >= 0) && (wert < 4))
                        feld[x][y][spieler] = wert;

    }

    public int getFeldgroesse() {
        return FELDGROESSE;
    }

    public int getAktiverSpieler() {return aktiverSpieler; }

    // gibt den Spielzustand zurück
    public int getSpielZustand(){
        return spielzustand;
    }

     // ******************************** public functions ********************************

    /*
     * Diese Funktion teilt der Spiellogik mit auf welches Feld der Spieler schießen will.
     *
     * Dabei wird das Ziel nur übernommen, wenn das Ziel auf dem Spielfeld liegt und noch nicht beschossen wurde.
     * Nur wenn erfolgreich ein neues Ziel gewählt wurde, wird das Spiel den Zustand "SPIELERSCHIESST" verlassen.
     *
     * @return:
     * true: Erfolgreich ein neues Ziel gewählt
     * return: Kein neues Ziel gewählt
     */

    public boolean setzeZiel(int x, int y, int spieler) {
        if (istImFeld(x,y)) {
            if(spieler == MENSCH) {
                if ((getFeld(x, y, COMPUTER) == WASSER) || (getFeld(x, y, COMPUTER) == SCHIFF)) {
                    spielerziel[0] = x;
                    spielerziel[1] = y;
                    zielGesetzt = true;
                    return true;
                }
            } else {
                if ((getFeld(x, y, MENSCH) == WASSER) || (getFeld(x, y, MENSCH) == SCHIFF)) {
                    computerziel[0] = x;
                    computerziel[1] = y;
                    zielGesetzt = true;
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * Diese Methode beinhaltet den gesamten Spielablauf.
     *
     * Es gibt insgesamt 8 Zustände:
     *
     * SPIELNEUSTART = 0;
     * Das Spiel wurde  initialisiert und der Spieler sieht die Positionen der eigenen Schiffe.
     * SPIELERSCHIESST = 1;
     * Der Spieler wählt ein Ziel aus, auf das geschossen werden soll. Bei Treffer wird im gleichen Zustand verblieben, da er nochmal schießen darf.
     * SPIELERVERFEHLT = 2;
     * Der Spieler hat verfehlt und sieht noch das Ergebnis des Schuss. Danach schießt der Computer.
     * COMPUTERSCHIESST = 3;
     * Der Computer hat geschossen auf ein Ziel und der Spieler sieht das Ergebnis des Schuss.
     * COMPUTERVERFEHLT = 4;
     * Der Computer hat verfehlt und der Spieler sieht noch das Ergebnis des Schuss. Danach schießt wieder der Spieler.
     * SPIELERGEWINNT = 5;
     * Der Spieler hat alle Schiffe zerstört. Danach wird zu SPIELNEUSTART übergegangen.
     * SPIELERVERLIERT = 6;
     * Der Computer hat alle Schiffe zerstört. Danach wird zu SPIELNEUSTART übergegangen.

     *
     */
    public int naechsterZustand() {

        switch (spielzustand) {
            case SPIELNEUSTART:
                // Spielstart - Der Spieler sieht das eigene Spielfeld und die Platzierung seiner Schiffe.

                // Vorbereitung für nächsten Zustand:
                // Spieler schießt, also soll das Computerfpielfeld angezeigt werden.
                aktiverSpieler = MENSCH;
                spielzustand=1;
                break;
            case SPIELERSCHIESST:
                //Spieler sieht das gegnerische Spielfeld und wählt ein Ziel zum Schießen aus.
                //Dieser Zustand kann nur verlassen werden, wenn die Funktion "setzeZiel(x,y)" erfolgreich ausgeführt wurde.

                // Vorbereitung für nächsten Zustand:
                // Der Spieler sieht noch mindestens einmal das Ergebnis des Schusses, also wird für den nächsten Zustand das Computerspielfeld angezeigt.
                aktiverSpieler = MENSCH;

                //Wenn ein Ziel gesetzt wurde, schieße.
                if (zielGesetzt) {
                    if (spielerSchiesst()) spielzustand = 1;
                    else spielzustand = 2;
                }
                // Überprüfe ob der Spieler gewonnen hat.
                if (hatGewonnen(MENSCH)) {
                    // Zeige Ergebnis
                    aktiverSpieler = NIEMAND;
                    spielzustand = 5;
                }
                break;
            case SPIELERVERFEHLT:
                // Spieler hat sieht das Ergebnis seines letzten Schusses

                // Vorbereitung für nächsten Zustand:
                // Computer wird schießen, also ist der Computer an der Reihe und das Spielerspielfeld wird angezeigt.
                aktiverSpieler = COMPUTER;
                spielzustand=3;
                break;
            case COMPUTERSCHIESST:
                // Spieler sieht Ergebnis das eigene Spielfeld und das Ergebnis des gegnerischen Schusses

                // Vorbereitung für nächsten Zustand:
                // Der Spieler sieht noch mindestens einmal das Ergebnis des Schusses, also ist noch der Computer an der Reihe und das Spielfeld des Spielers ist sichtbar
                aktiverSpieler = COMPUTER;

                // Wenn der Computer getroffen hat, bleibe in diesem Zustand
                if (computerSchiesst()){
                    spielzustand = 3;
                } else {
                    spielzustand = 4;
                }
                // Überprüfe ob der Computer gewonnen hat.
                if (hatGewonnen(COMPUTER)) {
                    // Zeige Ergebnis
                    aktiverSpieler = NIEMAND;
                    spielzustand=6;
                }
                break;
            case COMPUTERVERFEHLT:
                // Spieler hat sieht das Ergebnis des letzten Computerschusses

                // Vorbereitung für nächsten Zustand:
                // Im nächsten Zustand darf der Spieler wieder schießen, also Computerspielfeld anzeigen.
                aktiverSpieler = MENSCH;

                spielzustand=1;
                break;
            case SPIELERGEWINNT:
                // Gewonnen, der Spieler sieht das gegnerische Spielfeld

                // Vorbereitung für nächsten Zustand:
                // Spiel wird neugestartet, Mensch sieht seine Schiffe und schießt als erster.

                aktiverSpieler = MENSCH;


                spielende();
                spielzustand = 0;
                break;
            case SPIELERVERLIERT:
                // Verloren, der Spieler sieht die Positionen der gegnerischen Schiffe

                aktiverSpieler = MENSCH;

                spielende();
                spielzustand = 0;
                break;
            default: spielzustand = 0;
        }

        return spielzustand;
    }

    public int schiffZerstoert(int x, int y, int spieler) {
        int laenge = 0;

        boolean zerstoert = true;

        int naechstesFeld = getFeld(x,y,spieler);

        if ((naechstesFeld == SCHIFF) || (naechstesFeld == SCHIFFTREFFER)) {

            laenge++;
            int xplus = x + 1;
            int xminus = x - 1;
            int yplus = y + 1;
            int yminus = y - 1;

            naechstesFeld = getFeld(xplus, y, spieler);
            while ((naechstesFeld == SCHIFF) || (naechstesFeld == SCHIFFTREFFER)) {
                if (naechstesFeld == SCHIFF) zerstoert = false;
                xplus++;
                laenge++;
                naechstesFeld = getFeld(xplus, y, spieler);
            }

            naechstesFeld = getFeld(xminus, y, spieler);
            while ((naechstesFeld == SCHIFF) || (naechstesFeld == SCHIFFTREFFER)) {
                if (naechstesFeld == SCHIFF) zerstoert = false;
                xminus--;
                laenge++;
                naechstesFeld = getFeld(xminus, y, spieler);
            }

            naechstesFeld = getFeld(x, yplus, spieler);
            while ((naechstesFeld == SCHIFF) || (naechstesFeld == SCHIFFTREFFER)) {
                if (naechstesFeld == SCHIFF) zerstoert = false;
                yplus++;
                laenge++;
                naechstesFeld = getFeld(x, yplus, spieler);
            }

            naechstesFeld = getFeld(x, yminus, spieler);
            while ((naechstesFeld == SCHIFF) || (naechstesFeld == SCHIFFTREFFER)) {
                if (naechstesFeld == SCHIFF) zerstoert = false;
                yminus--;
                laenge++;
                naechstesFeld = getFeld(x, yminus, spieler);
            }
        }

        if (zerstoert) return laenge;
        return 0;
    }


    // gibt die Anzahl der Schiffe einer bestimmten Länge zurück
    public int getAnzahlSchiffe(int laenge, int spieler) {
        if ((laenge < schiffe.length) && (spieler < 2)) return schiffe[laenge][spieler];
        else return 0;
    }

    // gibt die Anzahl der Schiffe als formattierter String zurück

    public String schiffeToString(int spieler) {
        String schiffe = "";
        for (int laenge = MAX_LAENGE; laenge >= MIN_LAENGE; laenge--) {
            schiffe = schiffe + "Länge " + laenge + ": " + getAnzahlSchiffe(laenge,spieler) + "\n";
        }

        return schiffe;
    }

    public boolean istImFeld(int x, int y) {
        if ((x >= 0) && (x < FELDGROESSE) && (y >= 0) && (y < FELDGROESSE)) {
            return true;
        } else return false;
    }

    /*
      *  Platziert ein Schiff
      *
      *  Ein Schiff wird nur platziert, wenn die gesamte Schiffslänge auf dem Spielfeld liegt, und innerhalb von einem Feld Abstand kein anderes Schiff ist.
      *  Das Schiff wird ausgehend von der Position (x,y) in horizontal/vertikal aufsteigender Richtung platziert.
      *
      *  @params:
      *  laenge: Länge des Schiffs
      *  horizontal: true
      *  @return: Gibt zurück, ob das Schiff erfolgreich platziert wurde
      */
    public boolean platziereSchiff(int laenge, boolean horizontal, int x, int y, int spieler) {

        boolean schiffGesetzt = false;
        int horizont;
        if (horizontal) horizont = 1;
        else horizont = 0;
        int vertical = (horizont+1)%2;

        boolean blockiert = false;

        // Wenn das Schiff eine korrekte Länge hat
        if ((laenge >= MIN_LAENGE) && (laenge <= MAX_LAENGE)) {
            // wenn das Schiff auf dem Spielfeld liegt
            if (((x + horizont * laenge < FELDGROESSE) && (x >= 0)) && ((y + vertical * laenge < FELDGROESSE) && (y >= 0))) {
            /*
             * Für die jedes Feld bis zur Länge des Schiffs
             * da horizontal/vertikal auf 1 und 0 gesetzt werden, berechnet
             * x+horizontal*j bzw. y+vertical*j die j-te Position des Schiffs
             */
                for (int i = -1; i <= horizont * (laenge - 1) + 1; i++) {
                    for (int j = -1; j <= vertical * (laenge - 1) + 1; j++) {
                        //überprüfe ob es kein Wasserfeld ist oder außerhalb des Spielfelds liegt
                        int feld = getFeld(x + i, y + j, spieler);
                        if ((feld != WASSER) && (feld != -1)) {
                            blockiert = true;
                            break;
                        }
                    }
                    if (blockiert) break;
                }
            } else blockiert = true;
        } else blockiert = true;

        // wenn kein Feld blockiert ist
        if (!blockiert) {
            // für jedes Feld bis zur Länge des Schiffs
            for (int j = 0; j < laenge; j++) {
                /*
                 * setze ein Schifffeld
                 *
                 * da horizontal/vertikal auf 1 und 0 gesetzt werden, berechnet
                 * x+horizontal*j bzw. y+vertical*j die j-te Position des Schiffs
                 */
                feld[x + horizont * j][y + vertical * j][spieler] = SCHIFF;
            }
            schiffe[laenge][spieler]++;
            schiffGesetzt = true;
        }
        // gebe zurück, ob erfolgreich
        return schiffGesetzt;
    }

    // ******************************** private functions ********************************
    /*
     * Diese Funktion übernimmt alle Spielvorbereitungen für ein neues Spiel mit gleichen Parametern
     *
     * Für ein neues Spiel mit anderen Parametern, muss eine neue Instanz des Spiels erstellt werden.
     */
    private void initialize() {

        spielzustand = 0;

        zielGesetzt = false;
        spielerziel = new int[2];

        // Arraygröße für SpielActivity festlegen
        feld = new int[FELDGROESSE][FELDGROESSE][2];

        // für jeden Spieler
        for (int spieler = 0; spieler < 2; spieler++) {
            // für jedes SpielActivity
            for (int x = 0; x < FELDGROESSE; x++) {
                for (int y = 0; y < FELDGROESSE; y++) {
                    // setze als Wasserfeld
                    feld[x][y][spieler] = WASSER;
                }
            }
        }

        //initialisiere Schiffe-Array

        schiffe = new int[MAX_LAENGE +1][2];
        for (int i = 0; i < schiffe.length; i++){
            for (int j = 0; j < schiffe[i].length; j++) {
                schiffe[i][j] = 0;
            }
        }

        // für jeden Spieler
        for (int spieler = 0; spieler < 2; spieler++) {
            /*
             *  Platziere 1 Schiff der Maximallaenge
             *  Platziere 2 Schiffe der Maximallaenge-1
             *  ...
             *  Platziere n Schiffe der Minimallaenge
             */
            for (int laenge = MAX_LAENGE; laenge >=MIN_LAENGE ; laenge--) {
                for (int anzahl = MAX_LAENGE + 1 - laenge; anzahl >= 1; anzahl--) {
                    platziereZufallsSchiff(laenge, spieler);
                }
            }

        }
    }

    /*
     *  überprüft ob ein Spieler gewonnen hat
     *
     *  Wenn das SpielActivity des Gegners noch mindestens 1 Schifffeld hat, hat der Spieler noch nicht
     *  gewonnen.
     */
    private boolean hatGewonnen(int spieler) {

        boolean gewonnen = true;
        int gegner = gegner(spieler);

        // für jedes SpielActivity
        for (int x = 0; x < FELDGROESSE; x++) {
            for (int y = 0; y < FELDGROESSE; y++) {
                // Wenn es ein Schifffeld ist, hat der Spieler nicht gewonnen
                if (getFeld(x,y,gegner) == SCHIFF) {
                    gewonnen = false;
                    break;
                }
            }
            // wenn bereits ein Schifffeld gefunden wurde, breche Suche ab
            if (!gewonnen) break;
        }
        return gewonnen;
    }

    // Schieße auf das übergebene Feld und gebe zurück ob getroffen wurde
    private boolean spielerSchiesst() {
        zielGesetzt = false;
        return schuss(spielerziel[0],spielerziel[1], MENSCH);
        // Führe einen Schuss als Mensch aus und übernehme Rückgabewert
    }

    // Lasse Computer auf ein Feld schießen und gebe zurück ob getroffen wurde
    public boolean computerSchiesst() {
        return schuss(computerziel[0],computerziel[1], COMPUTER);
    }

    /* Überprüft, ob ein Feld ein zerstörtes Schiff beinhaltet und welche Länge es hatte
     *
     * @return:
     * 0 = Schiff nicht zerstört
     * n = Schiff der Länge n zerstört.
     */


    // Schießt auf ein SpielActivity und gibt zurück, ob Schuss erfolgreich war
    private boolean schuss(int x,int y, int spieler) {
        // bestimme Gegner des Spielers
        int gegner = gegner(spieler);

        boolean treffer = false;

        if (istImFeld(x,y)) {
            // Wenn Schiff, setze einen Schifftreffer und speichere, dass getroffen
            if (feld[x][y][gegner] == SCHIFF) {
                feld[x][y][gegner] = SCHIFFTREFFER;
                treffer = true;

                // Wenn das Schiff zerstört wurde, zähle die Schiffsanzahl herab
                int zerstoert = schiffZerstoert(x, y, gegner);
                if (zerstoert > 0) schiffe[zerstoert][gegner]--;
            }

            // Wenn Wasser, setze einen Wassertreffer
            if (feld[x][y][gegner] == WASSER) {
                feld[x][y][gegner] = WASSERTREFFER;
            }
        }
         // sonst behalte alten Wert

        // gebe zurück, ob getroffen wurde
        return treffer;

    }

    // Diese Methode beinhaltet alles, was bei Spielende geschehen soll.
    private void spielende() {
        // start Spiel neu
        initialize();
    }

    // Hilfsfunktion, um den Gegner eines Spielers zu bestimmen
    private int gegner(int spieler) {
        return (spieler+1)%2;
    }


    private void platziereZufallsSchiff(int laenge, int spieler) {
        boolean schiffGesetzt = false;

        // Für maximal 5000 Versuche
        for (int i = 0; i < 5000; i++){
            // Wenn noch nicht gesetzt, versuche ein Schiff zu setzen
            if (!schiffGesetzt) {

                // zufällige Ausrichtung bestimmen
                int richtung = rnd.nextInt(2);

                int x;
                int y;
                boolean horizontal;

                // zufällige Position bestimmen
                if (richtung == 1) {
                    horizontal = true;
                    x = rnd.nextInt(getFeldgroesse()-laenge);
                    y = rnd.nextInt(getFeldgroesse());
                } else {
                    horizontal = false;
                    x = rnd.nextInt(getFeldgroesse());
                    y = rnd.nextInt(getFeldgroesse()-laenge);
                }


                schiffGesetzt = platziereSchiff(laenge, horizontal,x,y,spieler);

                // Wenn erfolgreich gesetzt wurde, breche for-Schleife ab.
            } else break;
        }
    }
}


