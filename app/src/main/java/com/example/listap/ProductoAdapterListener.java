package com.example.listap;

public interface ProductoAdapterListener {
    void onProductoEstadoCambiado(Producto producto, boolean isChecked);
    void onProductoCantidadCambiada(Producto producto, int nuevaCantidad);
    void onProductoEliminar(Producto producto);
    void onProductoEditar(Producto producto);
}