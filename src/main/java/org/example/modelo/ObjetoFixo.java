package org.example.modelo;

// Elemento de "objetos_fixos" (muros). Bloqueiam movimento E radar.
// Campos confirmados no Swagger: id, type, model, x, y, z.
public class ObjetoFixo {

    private String id;
    private String type;
    private String model;
    private int x;
    private int y;
    private double z;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }
}