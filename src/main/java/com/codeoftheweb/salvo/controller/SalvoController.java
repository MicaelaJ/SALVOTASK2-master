package com.codeoftheweb.salvo.controller;

import com.codeoftheweb.salvo.models.*;
import com.codeoftheweb.salvo.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/api") //para cambiar la raiz de la ruta
public class SalvoController {

    /* permite que los objetos sean compartidos y administrados por el framework */
    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private ShipRepository shipRepository;

    @Autowired
    private SalvoRepository salvoRepository;

    @Autowired
    private ScoreRepository scoreRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /* ======================= GAMES ======================= */
    // dto con info de Player y todos los Games

    @RequestMapping("/games")
    public Map<String, Object> getAllGames(Authentication authentication) {
        Map<String, Object> dto = new LinkedHashMap<>();

        if (isGuest(authentication)) {
            dto.put("player", "Guest");
        } else {
            Player player = playerRepository.findByUserName(authentication.getName());
            dto.put("player", player.getPlayerDTO());
        }

        dto.put("games", gameRepository.findAll()
                .stream()
                .map(game -> makeGameDTO(game))
                .collect(toList()));

        return dto;
    }

    //Metodo que verifica si el usuario es Guest o User
    private boolean isGuest(Authentication authentication) {
        return authentication == null || authentication instanceof AnonymousAuthenticationToken;
    }

    /* ======================= GAME VIEW ======================= */
    // dto del gamePlayer con id igual al id que me pasan, si esta autorizado

    @RequestMapping("/game_view/{gamePlayer_Id}")
    public ResponseEntity<Map<String, Object>> GameView(@PathVariable Long gamePlayer_Id,
                                                        Authentication authentication) {

        Player player = playerRepository.findByUserName(authentication.getName());
        GamePlayer gamePlayer = gamePlayerRepository.findById(gamePlayer_Id).orElse(null);

        if (player != null) {
            if(gamePlayer   !=  null){
                if (gamePlayer.getPlayer().getId() == player.getId()) {
                    return new ResponseEntity<>(makeGameViewDTO(gamePlayer), HttpStatus.CREATED);
                }
            }

            return new ResponseEntity<>(makeMap("error", "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(makeMap("error", "Unauthorized"), HttpStatus.UNAUTHORIZED);
    }

    //Metodo para hacer DTO con key y value
    private Map<String, Object> makeMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    /* ======================= GameViewDTO======================= */

    /* obtiene datos del game del gamePlayer, da info sobre ambos gp en ese game
     * con dto de ships del gp principal y sus salvoes */

    public Map<String, Object> makeGameViewDTO(GamePlayer gamePlayer) {
        Map<String, Object> dto = new LinkedHashMap<>();
        Map<String, Object> opdto = new LinkedHashMap<>();
        GamePlayer opponent = getOpponent(gamePlayer);

        if (opponent != null) {
            opdto.put("self", getAllHits(getOpponent(gamePlayer)));
            opdto.put("opponent", getAllHits(opponent));
        } else {
            opdto.put("self", new ArrayList<>());
            opdto.put("opponent", new ArrayList<>());
        }

        Set<Ship> ships = gamePlayer.getShips();
        Set<Salvo> salvos = gamePlayer.getSalvos();
        dto.put("id", gamePlayer.getGame().getId());
        dto.put("created", gamePlayer.getGame().getCreationDate());
        dto.put("gameState", getGameState(gamePlayer));
        dto.put("gamePlayers", getAllGamePlayers(gamePlayer.getGame().getGamePlayers()));
        dto.put("ships", getAllShips(ships));
        dto.put("salvoes", getAllSalvos(salvos));
        dto.put("hits", getHitsDTO(gamePlayer));
        return dto;
    }

    /* ======================= Add Players ======================= */
    // Necesita username y password

    @RequestMapping(path = "/players", method = RequestMethod.POST)
    private ResponseEntity<Object> addPlayer(@RequestParam String email, @RequestParam String password) {

        if (email.isEmpty() || password.isEmpty()) {
            return new ResponseEntity<>("Missing data", HttpStatus.FORBIDDEN);
        }

        if (playerRepository.findByUserName(email) != null) {
            return new ResponseEntity<>("Name already exists", HttpStatus.FORBIDDEN);
        }

        playerRepository.save(new Player(email, passwordEncoder.encode(password)));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    //Metodo para hacer DTO con key y value
    private Map<String, Object> MakeMap(String key, String value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    /* ======================= Join Game ======================= */

    /*  Metodo que permite unirse a la partida ingresada por parametro
     * verifica player
     * "Join game" button en el front end */

    @RequestMapping(path = "/game/{gameid}/players", method = RequestMethod.POST)
    private ResponseEntity<Map<String, Object>> joinGame(@PathVariable Long gameid,
                                                         Authentication authentication) {

        if (authentication == null) {
            return new ResponseEntity<>(MakeMap("error", "No player logged in"), HttpStatus.UNAUTHORIZED);
        }
        Game joinGame = gameRepository.getOne(gameid);
        if (joinGame == null) {
            return new ResponseEntity<>(MakeMap("error", "No such game"), HttpStatus.FORBIDDEN);
        }
        if (joinGame.getGamePlayers().size() >= 2) {
            return new ResponseEntity<>(MakeMap("error", "Game is full"), HttpStatus.FORBIDDEN);
        }

        GamePlayer gamePlayer = gamePlayerRepository.save(new GamePlayer(joinGame, playerRepository.findByUserName(authentication.getName())));
        return new ResponseEntity<>(makeMap("gpid", gamePlayer.getId()), HttpStatus.CREATED);
    }

    /* ======================= Add Ships ======================= */

    /* Metodo que devuelve los ships del player pasado por parametro en la url
     *  verifica player
     * "add ships" button en el front end */

    @RequestMapping(path = "/games/players/{gamePlayerId}/ships", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> addShips(@PathVariable Long gamePlayerId,
                                                        @RequestBody Set<Ship> ships,
                                                        Authentication authentication) {
        GamePlayer gamePlayer = gamePlayerRepository.findById(gamePlayerId).get();
        Player player = playerRepository.findByUserName(authentication.getName());

        if (isGuest(authentication)) {
            return new ResponseEntity<>
                    (makeMap("error", "There is no current user logged in"), HttpStatus.UNAUTHORIZED);
        }
        if (!gamePlayerRepository.findById(gamePlayerId).isPresent()) {
            return new ResponseEntity<>
                    (makeMap("error", "There is no game player with the given ID"), HttpStatus.UNAUTHORIZED);
        }
        if (gamePlayer.getPlayer().getId() != player.getId()) {
            return new ResponseEntity<>(makeMap("error", "The current player is not the game player the ID references"),
                    HttpStatus.UNAUTHORIZED);
        }
        if (!gamePlayer.getShips().isEmpty()) {
            return new ResponseEntity<>
                    (makeMap("error", "The player already has ships placed"), HttpStatus.FORBIDDEN);
        }

        ships.forEach(ship -> ship.setGamePlayer(gamePlayer));
        shipRepository.saveAll(ships);
        return new ResponseEntity<>(makeMap("addShips", "Ships created"), HttpStatus.CREATED);
    }

    /* ======================= Add Salvos ======================= */

    /* verifica player
     * "add salvos" button en el front end. */

    @RequestMapping(path = "/games/players/{gamePlayerId}/salvos", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> addSalvoes(@PathVariable Long gamePlayerId,
                                                          @RequestBody Salvo salvo,
                                                          Authentication authentication) {

        GamePlayer gamePlayer = gamePlayerRepository.findById(gamePlayerId).get();
        Player player = playerRepository.findByUserName(authentication.getName());

        if (isGuest(authentication)) {
            return new ResponseEntity<>
                    (makeMap("error", "There is no current user logged in"), HttpStatus.UNAUTHORIZED);
        }

        if (!gamePlayerRepository.findById(gamePlayerId).isPresent()) {
            return new ResponseEntity<>
                    (makeMap("error", "There is no game player with the given ID"), HttpStatus.UNAUTHORIZED);
        }

        if (gamePlayer.getPlayer().getId() != player.getId()) {
            return new ResponseEntity<>(makeMap("error", "The current player is not the game player the ID references"),
                    HttpStatus.UNAUTHORIZED);
        }
/*
        salvo.setGamePlayer(gamePlayer);
        salvo.setTurn(gamePlayer.getSalvos().size()+1);
        salvoRepository.save(salvo);
        return new ResponseEntity<>(makeMap("addSalvoes", "Salvos save"), HttpStatus.CREATED);

 */

        Set<Salvo> salvoes = gamePlayer.getSalvos();
        for (Salvo salvoX : salvoes) {
            if (salvo.getTurn() == salvoX.getTurn() || gamePlayer.getSalvos().size() > getOpponent(gamePlayer).getSalvos().size()) {
                return new ResponseEntity<>
                        (makeMap("error", "The player already has submitted a salvo for the turn listed"),
                                HttpStatus.FORBIDDEN);
            }
        }

        salvoRepository.save(new Salvo(salvoes.size() + 1, gamePlayer, salvo.getSalvoLocations()));
        return new ResponseEntity<>(makeMap("OK", "Salvoes save"), HttpStatus.CREATED);

    }

    // Tabla de clasificasiones
    @RequestMapping("/leaderBoard")
    private List<Map<String, Object>> getLeaderBoard() {
        return playerRepository.findAll()
                .stream()
                .map(player -> playerLeaderBoardDTO(player))
                .collect(Collectors.toList());
    }

    /* ======================= GamePlayer DTOs ======================= */

    // Lista de dto de cada ship para cada gamePlayer
    private List<Map<String, Object>> getAllShips(Set<Ship> ships) {
        return ships
                .stream()
                .map(ship -> ship.shipDTO())
                .collect(Collectors.toList());
    }

    // Salvo Locations
    private List<Map<String, Object>> getAllSalvos(Set<Salvo> salvos) {
        return salvos
                .stream()
                .map(salvo -> salvo.salvoDTO())
                .collect(Collectors.toList());
    }

    // Score
    private List<Map<String, Object>> getAllScore(Set<Score> scores) {
        return scores
                .stream()
                .map(score -> score.scoreDTO())
                .collect(Collectors.toList());
    }

    private Map<String, Object> getPlayerScoreDTO(Player player) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("total", player.getTotalScore());
        dto.put("won", player.getWinScore());
        dto.put("lost", player.getLostScore());
        dto.put("tied", player.getTiedScore());
        return dto;
    }

    //LeaderBoard
    private Map<String, Object> playerLeaderBoardDTO(Player player) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", player.getId());
        dto.put("email", player.getUserName());
        dto.put("score", this.getPlayerScoreDTO(player));
        return dto;
    }

    private Map<String, Object> getHitsDTO(GamePlayer gamePlayer) {
        Map<String, Object> dto = new LinkedHashMap<>();

        GamePlayer opponent = getOpponent(gamePlayer);

        if (opponent != null) {
            dto.put("self", getAllHits(getOpponent(gamePlayer)));
            dto.put("opponent", getAllHits(gamePlayer));
        } else {
            dto.put("self", new ArrayList<>());
            dto.put("opponent", new ArrayList<>());
        }

        return dto;
    }


    private Map<String, Object> getDamageDTO(int carrierHIT, int carrierDMG, int battleshipHIT, int battleshipDMG,
                                             int submarineHIT, int submarineDMG, int destroyerHIT, int destroyerDMG,
                                             int patrolboatHIT, int patrolboatDMG) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("carrierHits", carrierHIT);
        dto.put("battleshipHits", battleshipHIT);
        dto.put("submarineHits", submarineHIT);
        dto.put("destroyerHits", destroyerHIT);
        dto.put("patrolboatHits", patrolboatHIT);
        dto.put("carrier", carrierDMG);
        dto.put("battleship", battleshipDMG);
        dto.put("submarine", submarineDMG);
        dto.put("destroyer", destroyerDMG);
        dto.put("patrolboat", patrolboatDMG);
        return dto;
    }
    private GamePlayer getOpponent(GamePlayer gamePlayer) {
        GamePlayer opponent = null;
        for (GamePlayer gp : gamePlayer.getGame().getGamePlayers()) {
            if (gp.getId() != gamePlayer.getId()) {
                opponent = gp;
            }
        }
        return opponent;
    }

    private List<Map<String, Object>> getAllHits(GamePlayer gamePlayer) {
        List<Map<String, Object>> listaDeDTO = new ArrayList<>();

        int carrierDMG = 0, battleshipDMG = 0, submarineDMG = 0, destroyerDMG = 0, patrolboatDMG = 0;
        for (Salvo salvo : gamePlayer.getSalvos()) {
            int carrierHIT = 0, battleshipHIT = 0, submarineHIT = 0, destroyerHIT = 0, patrolboatHIT = 0;
            List<String> hitLocations = new ArrayList<>();
            for (Ship ship : getOpponent(gamePlayer).getShips()) {
                List<String> hits = new ArrayList<>(salvo.getSalvoLocations());
                hits.retainAll(ship.getLocations());
                int shots = hits.size();
                if (shots != 0) {
                    hitLocations.addAll(hits);
                    switch (ship.getType()) {
                        case "carrier":
                            carrierHIT += shots;
                            carrierDMG += shots;
                            break;
                        case "battleship":
                            battleshipHIT += shots;
                            battleshipDMG += shots;
                            break;
                        case "submarine":
                            submarineHIT += shots;
                            submarineDMG += shots;
                            break;
                        case "destroyer":
                            destroyerHIT += shots;
                            destroyerDMG += shots;
                            break;
                        case "patrolboat":
                            patrolboatHIT += shots;
                            patrolboatDMG += shots;
                            break;
                    }
                }
            }
            Map<String, Object> dto = new LinkedHashMap<String, Object>();
            dto.put("turn", salvo.getTurn());
            dto.put("hitLocations", hitLocations);
            dto.put("damages", getDamageDTO(carrierHIT, carrierDMG, battleshipHIT, battleshipDMG,
                    submarineHIT, submarineDMG, destroyerHIT, destroyerDMG, patrolboatHIT, patrolboatDMG));
            dto.put("missed", salvo.getSalvoLocations().size() - hitLocations.size());
            listaDeDTO.add(dto);
        }
        return listaDeDTO;
    }

    private String getGameState(GamePlayer gamePlayer) {
        final int TOTAL_SHIP = 17;
        GamePlayer opp = getOpponent(gamePlayer);


        if(gamePlayer.getShips().isEmpty()){
            return "PLACESHIPS";
        }
        if(opp == null){
            return "WAITINGFOROPP";
        }
        if(opp.getShips().isEmpty()){
            return "WAIT";
        }

        int selfSalvoes = gamePlayer.getSalvos().size(), oppSalvoes = opp.getSalvos().size();
        if(selfSalvoes <= oppSalvoes && !gameOver(gamePlayer, opp, TOTAL_SHIP)){// && !gameOver(gp)){
            return "PLAY";
        }
        if (selfSalvoes > oppSalvoes && !gameOver(gamePlayer, opp, TOTAL_SHIP)){
            return "WAIT";
        }

        int totOpp = getTotal(opp), totSelf = getTotal(gamePlayer);
        if(totOpp == TOTAL_SHIP && totSelf < TOTAL_SHIP){
            return "WON";
        }

        if(totOpp == TOTAL_SHIP && totSelf == TOTAL_SHIP){
            return "TIE";
        }
        return "LOST";

    }

    private boolean gameOver(GamePlayer gp, GamePlayer opp, int largo) {
        if(getTotal(gp) == largo || getTotal(opp) == largo){
            return true;
        }
        return false;
    }

    private int getTotal(GamePlayer gp) {
        GamePlayer opp = getOpponent(gp);
        List<String> ships = new ArrayList<>();
        List<String> salvoes = new ArrayList<>();
        for (Ship ship: gp.getShips()) {
            ships.addAll(ship.getLocations());
        }
        for (Salvo salvo: getOpponent(gp).getSalvos()) {
            salvoes.addAll(salvo.getSalvoLocations());
        }

        ships.retainAll(salvoes);
        int total = ships.size();
        return total;
    }
    private List<Map<String, Object>> getSunks(GamePlayer gamePlayer) {
        List<Map<String, Object>> dto = new ArrayList<>();
        for (Salvo salvo : gamePlayer.getSalvos()) {
            List<String> hitLocations = new ArrayList<>();
            for (Ship ship : getOpponent(gamePlayer).getShips()) {
                List<String> hits = new ArrayList<>(salvo.getSalvoLocations());
                hits.retainAll(ship.getLocations());
                int shots = hits.size();
                if (shots != 0) {
                    hitLocations.addAll(hits);
                }
            }
        }

        return dto;
    }
/*
    // Hits dto
    public List<Map<String, Object>> makeHitsDTO(GamePlayer gamePlayer) {
        List<Map<String, Object>> list = new ArrayList<>();
        long
                destroyerDemage = 0,
                carrierDemage = 0,
                battleshipDemage = 0,
                submarineDemage = 0,
                patrolboatDemage = 0;

        for (Salvo salvo : getOpponent(gamePlayer).getSalvos()) {
            long
                    carrieHits = 0,
                    battleshipHits = 0,
                    submarineHits = 0,
                    destroyerHits = 0,
                    patrolboatHits = 0;


            Map<String, Object> dto = new LinkedHashMap<String, Object>();
            Map<String, Object> demageDto = new LinkedHashMap<>();

            dto.put("turn", salvo.getTurn());
            dto.put("hitLocations", getHitsLocations(gamePlayer, salvo));
            dto.put("damages", demageDto);
            dto.put("missed", salvo.getSalvoLocations().size() - getHitsLocations(gamePlayer, salvo).size());

            demageDto.put("carrierHits", getHitsType(gamePlayer, salvo, "Carrier"));
            demageDto.put("battleshipHits", getHitsType(gamePlayer, salvo, "Cattleship"));
            demageDto.put("submarineHits", getHitsType(gamePlayer, salvo, "Submarine"));
            demageDto.put("destroyerHits", getHitsType(gamePlayer, salvo, "Destroyer"));
            demageDto.put("patrolboatHits", getHitsType(gamePlayer, salvo, "Patrol Boat"));

            demageDto.put("carrier", carrierDemage + getHitsType(gamePlayer, salvo, "Carrier"));
            demageDto.put("battleship", battleshipDemage + getHitsType(gamePlayer, salvo, "Battleship"));
            demageDto.put("submarine", submarineDemage + getHitsType(gamePlayer, salvo, "Submarine"));
            demageDto.put("destroyer", destroyerDemage + getHitsType(gamePlayer, salvo, "Destroyer"));
            demageDto.put("patrolboat", patrolboatDemage + getHitsType(gamePlayer, salvo, "Patrol Boat"));

            list.add(dto);
        }

        return list;
    }

 */

    // Hits Type
    private long getHitsType(GamePlayer gamePlayer, Salvo salvo, String type) {
        List<String> ships = gamePlayer.getShips()
                .stream()
                .filter(ship -> ship.getType() == type)
                .flatMap(ship -> ship.getLocations().stream())
                .collect(toList());
        ships.retainAll(salvo.getSalvoLocations());

        if (ships.size() == 0) {
            return 0;

        }
        return ships.size();
    }

    // Hits Locations
    private List<String> getHitsLocations(GamePlayer gamePlayer, Salvo salvo) {
        List<String> ships = gamePlayer.getShips()
                .stream()
                .flatMap(ship -> ship.getLocations().stream())
                .collect(toList());
        ships.retainAll(salvo.getSalvoLocations());
        return ships;
    }

    /*  ======================= Game DTOs ======================= */

    // Game dto
    public Map<String, Object> makeGameDTO(Game game) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", game.getId());
        dto.put("created", game.getCreationDate());
        dto.put("gamePlayers", getAllGamePlayers(game.getGamePlayers()));
        dto.put("scores", getAllScore(game.getScores()));
        return dto;
    }

    // Lista de dtos de todos los gamePlayers
    private List<Map<String, Object>> getAllGamePlayers(Set<GamePlayer> gamePlayers) {
        return gamePlayers
                .stream()
                .map(gamePlayer -> gamePlayer.getGamePlayerDTO())
                .collect(Collectors.toList());
    }

}



