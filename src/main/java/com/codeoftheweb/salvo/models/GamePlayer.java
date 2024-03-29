package com.codeoftheweb.salvo.models;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.security.core.Authentication;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class GamePlayer {

    /* ======================= Atributos ======================= */

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    private Date joinDate;

    /* Metodo donde creo una relacion One to many entre GamePlayer y Player */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id")
    private Player player;

    /* Metodo donde creo una relacion One to many entre GamePlayer y Game */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "game_id")
    private Game game;

    /* Metodo donde creo una relacion One to many entre GamePlayer y Ship */
    @OneToMany(mappedBy = "gamePlayer", fetch = FetchType.EAGER)
    private Set<Ship> ships;

    /* Metodo donde creo una relacion One to many entre GamePlayer y Salvo */
    @OneToMany(mappedBy = "gamePlayer", fetch = FetchType.EAGER)
    private Set<Salvo> salvos;


    /* ======================= Constructor ======================= */

    public GamePlayer() {
    }

    public GamePlayer(Date joinDate, Game game, Player player) {
        this.joinDate = joinDate;
        this.game = game;
        this.player = player;
    }

    public GamePlayer(Game game, Player player) {
        this.joinDate = new Date();
        this.game = game;
        this.player = player;
    }

    /* ======================= Getters ======================= */

    public Long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public Date getJoinDate() {
        return joinDate;
    }

    public Game getGame() {
        return game;
    }

    public Set<Ship> getShips() {
        return ships;
    }

    public Set<Salvo> getSalvos() {
        return this.salvos;
    }


    /* ======================= Setters ======================= */

    public void setId(long id) {
        this.id = id;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setShips(Set<Ship> ships) {
        this.ships = ships;
    }

    public void setSalvos(Set<Salvo> salvos) {
        this.salvos = salvos;
    }

    public void setJoinDate(Date joinDate) {
        this.joinDate = joinDate;
    }

    /* =======================  DTO ======================= */

    public Map<String, Object> getGamePlayerDTO() {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", this.getId());
        dto.put("player", this.getPlayer().getPlayerDTO());
        dto.put("joinDate", this.getJoinDate());
        return dto;
    }

    /* Metodos */

    public void addShip(Ship ship) {
        this.ships.add(ship);
        ship.setGamePlayer(this);
    }

    public void addSalvo(Salvo salvo) {
        this.salvos.add(salvo);
        salvo.setGamePlayer(this);
    }
}






