package com.codeoftheweb.salvo.models;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class Ship {

    /* ======================= Atributos ======================= */

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;
    private String type;

    /* Metodo donde creo una relacion One to many entre Ship y GamePlayer */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gamePlayer_id")
    private GamePlayer gamePlayer;

    /* Metodo para ShipLocations */
    @ElementCollection
    @Column(name = "shipLocations")
    private Set<String> locations = new HashSet<>();

    public Map<String, Object> shipDTO() {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("type", this.getType());
        dto.put("locations", this.getLocations());
        return dto;
    }

    /* ======================= Constructor ======================= */

    public Ship() {
    }

    public Ship(GamePlayer gamePlayer, String type, Set<String> shipLocations) {
        this.gamePlayer = gamePlayer;
        this.locations = shipLocations;
        this.type = type;
    }

    /* ======================= Getters ======================= */

    public long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public GamePlayer getGamePlayer() {
        return gamePlayer;
    }

    public GamePlayer getShipPlayer() {
        return this.getGamePlayer();
    }

    public Set<String> getLocations() {
        return locations;
    }


    /* ======================= Setters ======================= */

    public void setId(long id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setGamePlayer(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
    }

    public void setShipLocations(Set<String> shipLocations) {
        this.locations = shipLocations;
    }

}
