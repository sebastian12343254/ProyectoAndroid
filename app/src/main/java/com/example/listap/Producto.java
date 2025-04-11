package com.example.listap;

import java.io.Serializable;
import java.util.Objects;

public class Producto implements Serializable {
    private String nombre;
    private double precio;
    private int cantidad;
    private boolean añadido;
    private String categoria;
    private String unidad;

    public Producto() {}

    public Producto(String nombre, double precio, int cantidad, boolean añadido, String categoria, String unidad) {
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
        this.añadido = añadido;
        this.categoria = categoria;
        this.unidad = (unidad == null || unidad.trim().isEmpty()) ? "uds." : unidad.trim();
    }

    public Producto(String nombreProducto, double precioProducto, int i, boolean b, String categoria) {
    }

    // Getters
    public String getNombre() { return nombre; }
    public double getPrecio() { return precio; }
    public int getCantidad() { return cantidad; }
    public boolean isAñadido() { return añadido; }
    public String getCategoria() { return categoria; }
    public String getUnidad() { return unidad; } // <-- GETTER PARA UNIDAD

    // Setters
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setPrecio(double precio) { this.precio = precio; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public void setAñadido(boolean añadido) { this.añadido = añadido; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setUnidad(String unidad) {
        this.unidad = (unidad == null || unidad.trim().isEmpty()) ? "uds." : unidad.trim();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Producto producto = (Producto) o;
        return nombre.equals(producto.nombre) && categoria.equals(producto.categoria);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre, categoria);
    }

    @Override
    public String toString() {
        return "Producto{" +
                "nombre='" + nombre + '\'' +
                ", precio=" + precio +
                ", cantidad=" + cantidad +
                ", añadido=" + añadido +
                ", categoria='" + categoria + '\'' +
                ", unidad='" + unidad + '\'' +
                '}';
    }
}