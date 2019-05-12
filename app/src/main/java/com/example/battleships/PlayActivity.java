package com.example.battleships;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.support.v7.widget.GridLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class PlayActivity extends AppCompatActivity {

    private String playerName;
    private TextView playerNameTextView;
    private TextView enemyNameTextView;
    private GridLayout gameBoard;
    private GridLayout gameBoardEnemy;

    private SquareButton[][] buttons;
    private SquareButton[][] buttonsEnemy;
    private Spiel spiel;
    private SpielNetworked spielNetworked;
    private SoundManager cannons;
    private boolean running = false;
    private Button restartButton;
    private TextView statusTextView;

    private BluetoothAdapter adapter;
    private BluetoothServerSocket hostSocket;
    private int hostType;

    private Connection connection = null;

    private long seed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        enemyNameTextView = findViewById(R.id.enemy_name);
        playerNameTextView = findViewById(R.id.player_name);
        gameBoard = findViewById(R.id.game_board);
        gameBoardEnemy = findViewById(R.id.game_board_enemy);
        restartButton = findViewById(R.id.restart_button);
        statusTextView = findViewById(R.id.status_label);
        cannons = new SoundManager(this);
        cannons.loadCannonSounds();

        Bundle extras = getIntent().getExtras();
        if(extras !=null) {
            playerName = extras.getString("PlayerName");
            hostType = extras.getInt("HostType");

        }

        playerNameTextView.setText(playerName);

        int numOfCol = gameBoard.getColumnCount();
        int numOfRow = gameBoard.getRowCount();
        buttons = new SquareButton[numOfRow][numOfCol];
        buttonsEnemy = new SquareButton[numOfRow][numOfCol];

        for(int yPos=0; yPos<numOfRow; yPos++){
            for(int xPos=0; xPos<numOfCol; xPos++){
                SquareButton button = new SquareButton(this);
                button.setEnabled(false);
                buttons[xPos][yPos] = button;
                gameBoard.addView(button);

                button = new SquareButton(this);
                buttonsEnemy[xPos][yPos] = button;
                gameBoardEnemy.addView(button);
            }
        }

        Log.d("BluetoothTest", "test");

        if(hostType == 0) {
            spiel = new Spiel();
            initializeListeners();
            start();
        } else {
            Log.d("BluetoothTest", "reached");
            setUpBluetooth();
            while(connection == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            runGameNetworked();
        }
    }

    public void initializeListeners() {
        for(int x=0; x<8; x++){
            for(int y=0; y<8; y++){
                final int a=x;
                final int b=y;
                buttonsEnemy[x][y].setOnClickListener( new View.OnClickListener(){
                    public void onClick(View v){
                        if(hostType == 0) {
                            update(a, b);
                        } else {
                            System.out.println("button pressed");
                            update_networked(a, b);
                        }
                    }
                });
            }
        }
    }


    public void drawOwnBoard() {
        for (int x = 0; x < spiel.getFeldgroesse(); x++) {
            for (int y = 0; y < spiel.getFeldgroesse(); y++) {

                // Zeichnet das Spielfeld des Spielers
                int fieldType = spiel.getFeld(x, y, Spiel.MENSCH);
                int fieldColor = Color.rgb(0, 120, 255);
                switch (fieldType) {
                    case Spiel.WASSERTREFFER:
                        fieldColor = Color.rgb(0, 80, 200);
                        break;
                    case Spiel.SCHIFF:
                        fieldColor = Color.rgb(100, 100, 100);
                        break;
                    case Spiel.SCHIFFTREFFER:
                        if(spiel.schiffZerstoert(x, y, Spiel.MENSCH) > 0) {
                            fieldColor = Color.rgb(150, 0, 0);
                        } else {
                            fieldColor = Color.rgb(255, 0, 0);
                        }
                }
                buttons[x][y].setBackgroundColor(fieldColor);
            }
        }
    }

    public void drawOwnBoardNetworked(SpielNetworked spiel, int spieler) {
        for (int x = 0; x < spiel.getFeldgroesse(); x++) {
            for (int y = 0; y < spiel.getFeldgroesse(); y++) {

                // Zeichnet das Spielfeld des Spielers
                int fieldType = spiel.getFeld(x, y, spieler);
                int fieldColor = Color.rgb(0, 120, 255);
                switch (fieldType) {
                    case SpielNetworked.WASSERTREFFER:
                        fieldColor = Color.rgb(0, 80, 200);
                        break;
                    case SpielNetworked.SCHIFF:
                        fieldColor = Color.rgb(100, 100, 100);
                        break;
                    case SpielNetworked.SCHIFFTREFFER:
                        if(spiel.schiffZerstoert(x, y, spieler) > 0) {
                            fieldColor = Color.rgb(150, 0, 0);
                        } else {
                            fieldColor = Color.rgb(255, 0, 0);
                        }
                }
                buttons[x][y].setBackgroundColor(fieldColor);
            }
        }
        gameBoard.invalidate();
    }

    public void drawEnemyBoard() {
        for (int x = 0; x < spiel.getFeldgroesse(); x++) {
            for (int y = 0; y < spiel.getFeldgroesse(); y++) {
                // Zeichnet das Spielfeld des Gegeners
                int fieldType = spiel.getFeld(x, y, Spiel.COMPUTER);
                int fieldColor = Color.rgb(0, 120, 255);
                switch (fieldType) {
                    case Spiel.WASSERTREFFER:
                        fieldColor = Color.rgb(0, 80, 200);
                        break;
                    case Spiel.SCHIFFTREFFER:
                        if(spiel.schiffZerstoert(x, y, Spiel.COMPUTER) > 0) {
                            fieldColor = Color.rgb(150, 0, 0);
                        } else {
                            fieldColor = Color.rgb(255, 0, 0);
                        }
                        break;
                }
                buttonsEnemy[x][y].setBackgroundColor(fieldColor);
            }
        }
    }

    public void drawEnemyBoardNetworked(SpielNetworked spiel, int spieler) {
        for (int x = 0; x < spiel.getFeldgroesse(); x++) {
            for (int y = 0; y < spiel.getFeldgroesse(); y++) {
                // Zeichnet das Spielfeld des Gegeners
                int fieldType = spiel.getFeld(x, y, spieler);
                int fieldColor = Color.rgb(0, 120, 255);
                switch (fieldType) {
                    case SpielNetworked.WASSERTREFFER:
                        fieldColor = Color.rgb(0, 80, 200);
                        break;
                    case SpielNetworked.SCHIFFTREFFER:
                        if(spiel.schiffZerstoert(x, y, spieler) > 0) {
                            fieldColor = Color.rgb(150, 0, 0);
                        } else {
                            fieldColor = Color.rgb(255, 0, 0);
                        }
                        break;
                }
                buttonsEnemy[x][y].setBackgroundColor(fieldColor);
            }
        }
        gameBoardEnemy.invalidate();
    }

    public void win(int player) {
        restartButton.setEnabled(true);
        setEnemyButtonsEnabled(false);
        switch (player) {
            case Spiel.COMPUTER:
                statusTextView.setText("Computer Wins!");
                break;
            case Spiel.MENSCH:
                statusTextView.setText("You win!");
        }
        spiel.naechsterZustand();
    }

    public void update(int x, int y) {
        if(!running) { return; }
        if(spiel.getSpielZustand() == Spiel.SPIELERSCHIESST &&
                (spiel.getFeld(x, y, Spiel.COMPUTER) == Spiel.WASSER) ||
                 spiel.getFeld(x, y, Spiel.COMPUTER) == Spiel.SCHIFF) {
            cannons.playRandomSound();
        }
        spiel.setzeZiel(x, y);
        spiel.naechsterZustand();
        drawEnemyBoard();

        if(spiel.getSpielZustand() == Spiel.SPIELERGEWINNT) {
            win(Spiel.MENSCH);
            running = false;
            return;
        }

        while(spiel.getSpielZustand() != Spiel.SPIELERSCHIESST) {
            spiel.naechsterZustand();
            if(spiel.getSpielZustand() == Spiel.SPIELERVERLIERT) {
                win(Spiel.COMPUTER);
                running = false;
                return;
            }
            drawOwnBoard();
        }
    }

    public void update_networked(int x, int y) {
        if(!running) { return; }
        if(hostType == 1) {
            if (spielNetworked.getSpielZustand() == SpielNetworked.SPIELERSCHIESST &&
                    (spielNetworked.getFeld(x, y, SpielNetworked.COMPUTER) == SpielNetworked.WASSER) ||
                    spielNetworked.getFeld(x, y, SpielNetworked.COMPUTER) == SpielNetworked.SCHIFF) {
                cannons.playRandomSound();
                spielNetworked.setzeZiel(x, y, SpielNetworked.MENSCH);
                spielNetworked.naechsterZustand();
                drawEnemyBoardNetworked(spielNetworked, SpielNetworked.COMPUTER);
                connection.send(x + ":" + y);
            }

            // TODO
            if (spielNetworked.getSpielZustand() == SpielNetworked.SPIELERGEWINNT) {
                win(Spiel.MENSCH);
                running = false;
                return;
            }

            while (spielNetworked.getSpielZustand() != SpielNetworked.SPIELERSCHIESST) {
                if (spielNetworked.getSpielZustand() == SpielNetworked.COMPUTERSCHIESST) {
                    String[] shotParts = connection.receive().split(":");
                    spielNetworked.setzeZiel(Integer.parseInt(shotParts[0]), Integer.parseInt(shotParts[1]), SpielNetworked.COMPUTER);
                }
                spielNetworked.naechsterZustand();
                drawOwnBoardNetworked(spielNetworked, SpielNetworked.MENSCH);

                //TODO
                if (spielNetworked.getSpielZustand() == SpielNetworked.SPIELERVERLIERT) {
                    win(SpielNetworked.COMPUTER);
                    running = false;
                    return;
                }
            }
        } else {
            if (spielNetworked.getSpielZustand() == SpielNetworked.COMPUTERSCHIESST &&
                    (spielNetworked.getFeld(x, y, SpielNetworked.MENSCH) == SpielNetworked.WASSER) ||
                    spielNetworked.getFeld(x, y, SpielNetworked.MENSCH) == SpielNetworked.SCHIFF) {
                cannons.playRandomSound();
                spielNetworked.setzeZiel(x, y, SpielNetworked.COMPUTER);
                spielNetworked.naechsterZustand();
                drawEnemyBoardNetworked(spielNetworked, SpielNetworked.MENSCH);
                connection.send(x + ":" + y);
            }

            // TODO
            if (spielNetworked.getSpielZustand() == SpielNetworked.SPIELERVERLIERT) {
                win(Spiel.MENSCH);
                running = false;
                return;
            }

            while (spielNetworked.getSpielZustand() != SpielNetworked.COMPUTERSCHIESST) {
                if (spielNetworked.getSpielZustand() == SpielNetworked.SPIELERSCHIESST) {
                    String[] shotParts = connection.receive().split(":");
                    spielNetworked.setzeZiel(Integer.parseInt(shotParts[0]), Integer.parseInt(shotParts[1]), SpielNetworked.MENSCH);
                }
                spielNetworked.naechsterZustand();
                drawOwnBoardNetworked(spielNetworked, SpielNetworked.COMPUTER);

                //TODO
                if (spielNetworked.getSpielZustand() == SpielNetworked.SPIELERGEWINNT) {
                    win(SpielNetworked.COMPUTER);
                    running = false;
                    return;
                }
            }
        }
    }

    public void restart(View view) {
        start();
    }

    private void start() {
        restartButton.setEnabled(false);
        setEnemyButtonsEnabled(true);
        running = true;
        spiel.naechsterZustand();
        statusTextView.setText("Your turn");
        drawOwnBoard();
        drawEnemyBoard();
    }

    public void setEnemyButtonsEnabled(boolean enabled) {
        for(Button[] bs: buttonsEnemy) {
            for(Button b: bs) {
                b.setEnabled(enabled);
            }
        }
    }

    private void setUpBluetooth() {
        adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice opponent = null;
        if(!adapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, 1);
        }

        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            for(BluetoothDevice device : pairedDevices) {
                if(device.getName().contains("Battleship")) {
                    opponent = device;
                    break;
                }
            }
        }

        if(opponent != null) {
            if(hostType == 1) {
                hostGame();
            } else {
                connectGame(opponent);
            }
        }
    }

    private void hostGame() {
        new AcceptThread().start();
    }

    private void connectGame(BluetoothDevice opponent) {
        new ConnectThread(opponent).start();
    }

    public void runGameNetworked() {
        // Transfer names and seed
        statusTextView.setText("Connected");
        if(hostType == 1) {
            connection.send(playerName);
            enemyNameTextView.setText(connection.receive());
            seed = System.currentTimeMillis();
            connection.send(Long.toString(seed));
        } else {
            enemyNameTextView.setText(connection.receive());
            connection.send(playerName);
            seed = Long.parseLong(connection.receive());
        }

        spielNetworked = new SpielNetworked(8,4,2, seed);
        initializeListeners();

        if(hostType == 1) {
            drawEnemyBoardNetworked(spielNetworked, SpielNetworked.COMPUTER);
            drawOwnBoardNetworked(spielNetworked, SpielNetworked.MENSCH);
        } else {
            drawEnemyBoardNetworked(spielNetworked, SpielNetworked.MENSCH);
            drawOwnBoardNetworked(spielNetworked, SpielNetworked.COMPUTER);
        }
        spielNetworked.naechsterZustand();
        if(hostType == 2) {
            drawEnemyBoardNetworked(spielNetworked, SpielNetworked.COMPUTER);
            drawOwnBoardNetworked(spielNetworked, SpielNetworked.MENSCH);
            while (spielNetworked.getSpielZustand() != SpielNetworked.COMPUTERSCHIESST) {
                if (spielNetworked.getSpielZustand() == SpielNetworked.SPIELERSCHIESST) {
                    String[] shotParts = connection.receive().split(":");
                    spielNetworked.setzeZiel(Integer.parseInt(shotParts[0]), Integer.parseInt(shotParts[0]), SpielNetworked.MENSCH);
                }
                spielNetworked.naechsterZustand();

                //TODO
                if (spielNetworked.getSpielZustand() == SpielNetworked.SPIELERGEWINNT) {
                    win(SpielNetworked.COMPUTER);
                    running = false;
                    return;
                }
                drawOwnBoardNetworked(spielNetworked, SpielNetworked.MENSCH);
            }
        }
        running = true;
    }

    public void shutdown() {
        if(hostSocket != null) {
            try {
                hostSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        connection.close();
    }

    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mmServerSocket;
        private final String NAME = "HOST";
        private final UUID MY_UUID = UUID.fromString("625f20e3-7e59-4e77-a5d5-edd912f6c894");
        private final String TAG = "BT_HOST";

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = adapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                System.out.println("listening");
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    System.out.println("Socket's accept() method failed");
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    hostSocket = mmServerSocket;
                    connection = new Connection(socket);
                    System.out.println("client connected to server");
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        private final UUID MY_UUID = UUID.fromString("625f20e3-7e59-4e77-a5d5-edd912f6c894");
        private final String TAG = "BT_HOST";

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            adapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connection = new Connection(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }
}
