package com.example.listap;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List; // Usar List es más genérico
import java.util.Locale;

public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.ViewHolder> {

    private List<Producto> productosMostrados; // Solo la lista a mostrar
    private final ProductoAdapterListener listener; // Interfaz para comunicar eventos

    // Constructor recibe la lista inicial (puede estar vacía) y el listener
    public ProductoAdapter(List<Producto> productosIniciales, ProductoAdapterListener listener) {
        this.productosMostrados = new ArrayList<>(productosIniciales); // Copia defensiva
        this.listener = listener;
    }

    // Método para actualizar la lista mostrada desde la Activity
    public void submitList(List<Producto> nuevaLista) {
        productosMostrados.clear();
        productosMostrados.addAll(nuevaLista);
        notifyDataSetChanged(); // Notificar al RecyclerView sobre los cambios
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_producto, parent, false);
        return new ViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Producto producto = productosMostrados.get(position);
        holder.bind(producto, listener); // Pasar producto y listener al ViewHolder
    }

    @Override
    public int getItemCount() {
        return productosMostrados.size();
    }

    // ViewHolder ahora maneja la lógica de binding y listeners internos
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtPrecio, txtUnidad; // <-- AÑADIR txtUnidad
        EditText editCantidad;
        CheckBox checkAñadido;
        ImageButton btnEliminar, btnEditar;
        TextWatcher cantidadWatcher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombre = itemView.findViewById(R.id.txtNombre);
            txtPrecio = itemView.findViewById(R.id.txtPrecio);
            editCantidad = itemView.findViewById(R.id.editCantidad);
            checkAñadido = itemView.findViewById(R.id.checkAñadido);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            txtUnidad = itemView.findViewById(R.id.txtUnidad); // <-- OBTENER REFERENCIA
        }

        public void bind(final Producto producto, final ProductoAdapterListener listener) {
            txtNombre.setText(producto.getNombre());
            txtPrecio.setText(String.format(Locale.getDefault(),"Precio: $%.2f", producto.getPrecio())); // Usar Locale Default para mostrar moneda

            // --- CheckBox Añadido ---
            checkAñadido.setOnCheckedChangeListener(null);
            checkAñadido.setChecked(producto.isAñadido());
            checkAñadido.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (producto.isAñadido() != isChecked) {
                    listener.onProductoEstadoCambiado(producto, isChecked);
                }
            });

            // --- EditText Cantidad ---
            if (cantidadWatcher != null) {
                editCantidad.removeTextChangedListener(cantidadWatcher);
            }
            editCantidad.setText(String.valueOf(producto.getCantidad()));
            cantidadWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    int nuevaCantidad;
                    try {
                        nuevaCantidad = Integer.parseInt(s.toString());
                        if (nuevaCantidad < 0) nuevaCantidad = 0;
                    } catch (NumberFormatException e) {
                        nuevaCantidad = 0; // O 1 si prefieres
                    }
                    if (producto.getCantidad() != nuevaCantidad) {
                        listener.onProductoCantidadCambiada(producto, nuevaCantidad);
                    }
                }
            };
            editCantidad.addTextChangedListener(cantidadWatcher);

            // --- TextView Unidad ---
            String unidad = producto.getUnidad();
            // Mostrar la unidad si existe y no es la default "uds." (o si prefieres mostrar siempre "uds.")
            if (unidad != null && !unidad.isEmpty()) {
                txtUnidad.setText(unidad);
                txtUnidad.setVisibility(View.VISIBLE); // Asegurarse que sea visible
            } else {
                // Si la unidad es null o vacía, podríamos ocultarla o mostrar "uds."
                // txtUnidad.setVisibility(View.GONE); // Opción 1: Ocultar
                txtUnidad.setText("uds."); // Opción 2: Mostrar default
                txtUnidad.setVisibility(View.VISIBLE);
            }


            // --- Botones ---
            btnEliminar.setOnClickListener(v -> listener.onProductoEliminar(producto));
            btnEditar.setOnClickListener(v -> listener.onProductoEditar(producto));
        }
    }
}