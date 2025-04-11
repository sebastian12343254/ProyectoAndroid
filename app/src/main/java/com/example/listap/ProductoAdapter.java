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
import java.util.List;
import java.util.Locale;

public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.ViewHolder> {

    private List<Producto> productosMostrados;
    private final ProductoAdapterListener listener;

    public ProductoAdapter(List<Producto> productosIniciales, ProductoAdapterListener listener) {
        this.productosMostrados = new ArrayList<>(productosIniciales);
        this.listener = listener;
    }

    // Método para actualizar la lista mostrada desde la Activity
    public void submitList(List<Producto> nuevaLista) {
        productosMostrados.clear();
        productosMostrados.addAll(nuevaLista);
        notifyDataSetChanged();
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
        holder.bind(producto, listener);
    }

    @Override
    public int getItemCount() {
        return productosMostrados.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtPrecio, txtUnidad;
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
            txtUnidad = itemView.findViewById(R.id.txtUnidad);
        }

        public void bind(final Producto producto, final ProductoAdapterListener listener) {
            txtNombre.setText(producto.getNombre());
            txtPrecio.setText(String.format(Locale.getDefault(),"Precio: $%.2f", producto.getPrecio()));

            // --- CheckBox Añadido ---
            checkAñadido.setOnCheckedChangeListener(null);
            checkAñadido.setChecked(producto.isAñadido());
            checkAñadido.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (producto.isAñadido() != isChecked) {
                    listener.onProductoEstadoCambiado(producto, isChecked);
                }
            });

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

            String unidad = producto.getUnidad();
            if (unidad != null && !unidad.isEmpty()) {
                txtUnidad.setText(unidad);
                txtUnidad.setVisibility(View.VISIBLE);
            } else {
                txtUnidad.setText("uds.");
                txtUnidad.setVisibility(View.VISIBLE);
            }

            btnEliminar.setOnClickListener(v -> listener.onProductoEliminar(producto));
            btnEditar.setOnClickListener(v -> listener.onProductoEditar(producto));
        }
    }
}