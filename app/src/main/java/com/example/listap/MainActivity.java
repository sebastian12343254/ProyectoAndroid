package com.example.listap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements ProductoAdapterListener {


    private static final String PREFS_NAME = "ListaPrefs";
    private static final String KEY_LISTA_PRODUCTOS = "listaProductos";

    private RecyclerView recyclerView;
    private Button btnVerAñadidos, btnVerNoAñadidos, btnVerTodos, btnAgregar, btnAcercaDe;
    private TextView totalText;
    private Spinner spinnerOrdenar, spinnerCategoria;
    private EditText searchBar;

    private ArrayList<Producto> listaProductos = new ArrayList<>();
    private ProductoAdapter adapter;
    private Gson gson = new Gson();

    private String categoriaSeleccionada = "Todos";
    private String filtroBusqueda = "";
    private EstadoFiltro estadoFiltro = EstadoFiltro.TODOS;
    private int ordenSeleccionado = 0;

    private enum EstadoFiltro { TODOS, ANADIDOS, NO_ANADIDOS }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnAcercaDe = findViewById(R.id.btnAcercaDe);

        btnAcercaDe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AcercaDe();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.backgroundColor));
        }


        loadListaProductos();

        initializeUIComponents();


        setupRecyclerView();

        setupSpinners();

        setupSearchBar();

        setupFiltroEstadoButtons();

        btnAgregar.setOnClickListener(v -> mostrarDialogoAgregarOEditar(null)); // null para modo agregar

        aplicarFiltrosYSort();
        actualizarTotal();
    }



    private void initializeUIComponents() {
        totalText = findViewById(R.id.totalText);
        recyclerView = findViewById(R.id.recyclerView);
        btnVerTodos = findViewById(R.id.btnVerTodos);
        btnVerAñadidos = findViewById(R.id.btnVerAñadidos);
        btnVerNoAñadidos = findViewById(R.id.btnVerNoAñadidos);
        btnAgregar = findViewById(R.id.btnAgregar);
        spinnerOrdenar = findViewById(R.id.spinnerOrdenar);
        spinnerCategoria = findViewById(R.id.spinnerCategoria);
        searchBar = findViewById(R.id.searchBar);

    }

    private void setupRecyclerView() {
        adapter = new ProductoAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> adapterCategoria = ArrayAdapter.createFromResource(this,
                R.array.categorias, android.R.layout.simple_spinner_item);
        adapterCategoria.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(adapterCategoria);
        spinnerCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                categoriaSeleccionada = parentView.getItemAtPosition(position).toString();
                aplicarFiltrosYSort(); // Volver a filtrar y ordenar
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}
        });

        ArrayAdapter<CharSequence> adapterOrdenar = ArrayAdapter.createFromResource(this,
                R.array.opciones_ordenar, android.R.layout.simple_spinner_item);
        adapterOrdenar.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrdenar.setAdapter(adapterOrdenar);
        spinnerOrdenar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                ordenSeleccionado = position;
                aplicarFiltrosYSort();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}
        });
    }

    private void setupSearchBar() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtroBusqueda = s.toString().toLowerCase().trim();
                aplicarFiltrosYSort();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFiltroEstadoButtons() {
        btnVerTodos.setOnClickListener(v -> setEstadoFiltro(EstadoFiltro.TODOS));
        btnVerAñadidos.setOnClickListener(v -> setEstadoFiltro(EstadoFiltro.ANADIDOS));
        btnVerNoAñadidos.setOnClickListener(v -> setEstadoFiltro(EstadoFiltro.NO_ANADIDOS));
    }

    private void setEstadoFiltro(EstadoFiltro nuevoEstado) {
        if (estadoFiltro != nuevoEstado) {
            estadoFiltro = nuevoEstado;
            aplicarFiltrosYSort();
        }
    }


    private void aplicarFiltrosYSort() {
        List<Producto> listaFiltrada = new ArrayList<>(listaProductos);

        if (!"Todos".equals(categoriaSeleccionada)) {
            listaFiltrada = listaFiltrada.stream()
                    .filter(p -> p.getCategoria().equals(categoriaSeleccionada))
                    .collect(Collectors.toList());
        }

        if (!filtroBusqueda.isEmpty()) {
            listaFiltrada = listaFiltrada.stream()
                    .filter(p -> p.getNombre().toLowerCase().contains(filtroBusqueda))
                    .collect(Collectors.toList());
        }

        if (estadoFiltro != EstadoFiltro.TODOS) {
            boolean buscarAnadidos = (estadoFiltro == EstadoFiltro.ANADIDOS);
            listaFiltrada = listaFiltrada.stream()
                    .filter(p -> p.isAñadido() == buscarAnadidos)
                    .collect(Collectors.toList());
        }

        ordenarLista(listaFiltrada);

        adapter.submitList(listaFiltrada);
    }

    private void ordenarLista(List<Producto> lista) {
        Comparator<Producto> comparator;
        switch (ordenSeleccionado) {
            case 1:
                comparator = Comparator.comparingDouble(Producto::getPrecio);
                break;
            case 2:
                // Manejar explícitamente los nombres nulos (poco probable, pero seguro)
                comparator = (p1, p2) -> {
                    String nombre1 = p1.getNombre();
                    String nombre2 = p2.getNombre();
                    if (nombre1 == null) return (nombre2 == null) ? 0 : 1;
                    if (nombre2 == null) return -1;
                    return nombre2.compareToIgnoreCase(nombre1);
                };
                break;
            case 3:
                comparator = (p1, p2) -> Double.compare(p2.getPrecio(), p1.getPrecio());
                break;
            case 0:
            default:
                // Manejar explícitamente los nombres nulos (poco probable, pero seguro)
                comparator = (p1, p2) -> {
                    String nombre1 = p1.getNombre();
                    String nombre2 = p2.getNombre();
                    if (nombre1 == null) return (nombre2 == null) ? 0 : 1;
                    if (nombre2 == null) return -1;
                    return nombre1.compareToIgnoreCase(nombre2);
                };
                break;
        }
        Collections.sort(lista, comparator);
    }

    @Override
    public void onProductoEstadoCambiado(Producto producto, boolean isChecked) {
        Producto productoEnLista = findProductoInMasterList(producto);
        if (productoEnLista != null && productoEnLista.isAñadido() != isChecked) {
            productoEnLista.setAñadido(isChecked);
            saveListaProductos();
            actualizarTotal();
            if (estadoFiltro != EstadoFiltro.TODOS) {
                aplicarFiltrosYSort();
            }

        }
    }

    @Override
    public void onProductoCantidadCambiada(Producto producto, int nuevaCantidad) {
        Producto productoEnLista = findProductoInMasterList(producto);
        if (productoEnLista != null && productoEnLista.getCantidad() != nuevaCantidad) {
            productoEnLista.setCantidad(nuevaCantidad);
            saveListaProductos();
            if (productoEnLista.isAñadido()) {
                actualizarTotal();
            }
        }
    }

    @Override
    public void onProductoEliminar(final Producto producto) {

        new AlertDialog.Builder(this)
                .setTitle("Confirmar Eliminación")
                .setMessage("¿Seguro que quieres eliminar '" + producto.getNombre() + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    boolean removed = listaProductos.remove(producto); // Usa equals()
                    if (removed) {
                        saveListaProductos();
                        aplicarFiltrosYSort();
                        actualizarTotal();
                    } else {
                        Log.w("MainActivity", "Producto a eliminar no encontrado en lista maestra: " + producto.getNombre());
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onProductoEditar(Producto producto) {
        Producto productoParaEditar = findProductoInMasterList(producto);
        if (productoParaEditar != null) {
            mostrarDialogoAgregarOEditar(productoParaEditar);
        } else {
            Log.e("MainActivity", "No se encontró el producto a editar en la lista maestra: " + producto.getNombre());
            Toast.makeText(this, "Error al intentar editar el producto.", Toast.LENGTH_SHORT).show();
        }
    }

    private Producto findProductoInMasterList(Producto productoToFind) {
        for (Producto p : listaProductos) {
            if (p.equals(productoToFind)) {
                return p;
            }
        }
        return null;
    }

    private void mostrarDialogoAgregarOEditar(final Producto productoAEditar) {
        boolean isEditing = productoAEditar != null;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_producto, null);
        final TextInputLayout inputLayoutNombre = dialogView.findViewById(R.id.inputLayoutNombre);
        final TextInputEditText editNombre = dialogView.findViewById(R.id.editNombre);
        final TextInputLayout inputLayoutPrecio = dialogView.findViewById(R.id.inputLayoutPrecio);
        final TextInputEditText editPrecio = dialogView.findViewById(R.id.editPrecio);
        final TextInputLayout inputLayoutUnidad = dialogView.findViewById(R.id.inputLayoutUnidad);
        final TextInputEditText editUnidad = dialogView.findViewById(R.id.editUnidad);
        final Spinner spinnerCategoriaAgregar = dialogView.findViewById(R.id.spinnerCategoriaAgregar);

        ArrayAdapter<CharSequence> adapterCategoriaOriginal = ArrayAdapter.createFromResource(this,
                R.array.categorias, android.R.layout.simple_spinner_item);
        List<String> categoriasDialogo = new ArrayList<>();
        for (int i = 0; i < adapterCategoriaOriginal.getCount(); i++) {
            String item = adapterCategoriaOriginal.getItem(i).toString();
            if (!item.equals("Todos")) {
                categoriasDialogo.add(item);
            }
        }
        ArrayAdapter<String> adapterDialogo = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoriasDialogo);
        adapterDialogo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoriaAgregar.setAdapter(adapterDialogo);

        if (isEditing) {
            editNombre.setText(productoAEditar.getNombre());
            editPrecio.setText(String.format(Locale.US, "%.2f", productoAEditar.getPrecio()));
            String unidadActual = productoAEditar.getUnidad();
            if (unidadActual != null && !unidadActual.equalsIgnoreCase("uds.")) {
                editUnidad.setText(unidadActual);
            } else {
                editUnidad.setText("");
            }
            int spinnerPosition = adapterDialogo.getPosition(productoAEditar.getCategoria());
            spinnerCategoriaAgregar.setSelection(spinnerPosition >= 0 ? spinnerPosition : 0);
        } else {
            editUnidad.setText("");
        }

        new AlertDialog.Builder(this)
                .setTitle(isEditing ? R.string.dialog_title_editar : R.string.dialog_title_agregar)
                .setView(dialogView)
                .setPositiveButton(isEditing ? R.string.dialog_button_guardar : R.string.dialog_button_agregar, (dialog, which) -> {
                    String nombre = editNombre.getText().toString().trim();
                    String precioStr = editPrecio.getText().toString().trim();
                    String unidad = editUnidad.getText().toString().trim();
                    String categoriaSeleccionadaDialogo = spinnerCategoriaAgregar.getSelectedItem().toString();

                    boolean valido = true;

                    if (TextUtils.isEmpty(nombre)) {
                        inputLayoutNombre.setError(getString(R.string.toast_nombre_vacio));
                        valido = false;
                    } else {
                        inputLayoutNombre.setError(null);
                    }

                    double precio = 0;
                    try {
                        precio = Double.parseDouble(precioStr);
                        if (precio < 0) {
                            inputLayoutPrecio.setError(getString(R.string.toast_precio_invalido));
                            valido = false;
                        } else {
                            inputLayoutPrecio.setError(null);
                        }
                    } catch (NumberFormatException e) {
                        inputLayoutPrecio.setError(getString(R.string.toast_precio_invalido));
                        valido = false;
                    }

                    if (!valido) {
                        return;
                    }

                    if (isEditing) {
                        productoAEditar.setNombre(nombre);
                        productoAEditar.setPrecio(precio);
                        productoAEditar.setCategoria(categoriaSeleccionadaDialogo);
                        productoAEditar.setUnidad(unidad.isEmpty() ? "uds." : unidad); // Valor por defecto
                    } else {
                        int cantidadInicial = 1;
                        Producto nuevo = new Producto(nombre, precio, cantidadInicial, false, categoriaSeleccionadaDialogo,
                                unidad.isEmpty() ? "uds." : unidad); // Valor por defecto
                        listaProductos.add(nuevo);
                    }

                    saveListaProductos();
                    aplicarFiltrosYSort();
                    actualizarTotal();
                })
                .setNegativeButton(R.string.dialog_button_cancelar, null)
                .show();
    }

    private void actualizarTotal() {
        double total = 0;
        for (Producto p : listaProductos) {
            if (p.isAñadido()) {
                total += p.getPrecio() * Math.max(0, p.getCantidad());
            }
        }
        totalText.setText(String.format(Locale.getDefault(), "Total: $%.2f", total));
    }

    private void saveListaProductos() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        try {
            String jsonLista = gson.toJson(listaProductos);
            editor.putString(KEY_LISTA_PRODUCTOS, jsonLista);
            editor.apply();
            Log.d("MainActivity", "Lista guardada correctamente. Tamaño: " + listaProductos.size());
        } catch (Exception e) {
            Log.e("MainActivity", "Error al guardar la lista en JSON", e);
        }
    }

    private void loadListaProductos() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonLista = prefs.getString(KEY_LISTA_PRODUCTOS, null);

        if (jsonLista != null) {
            Type type = new TypeToken<ArrayList<Producto>>() {}.getType();
            try {
                listaProductos = gson.fromJson(jsonLista, type);

                if (listaProductos == null) {
                    listaProductos = new ArrayList<>();
                    Log.w("MainActivity", "El JSON cargado resultó en una lista nula, iniciando lista vacía.");
                } else {
                    // Asegurarse de que no haya campos nulos que puedan causar problemas
                    for (Producto p : listaProductos) {
                        if (p.getNombre() == null) p.setNombre("");
                        if (p.getCategoria() == null) p.setCategoria("Otros");
                        if (p.getUnidad() == null) p.setUnidad("uds.");
                    }
                }
                Log.d("MainActivity", "Lista cargada desde SharedPreferences. Tamaño: " + listaProductos.size());
            } catch (Exception e) {
                Log.e("MainActivity", "Error al cargar/deserializar lista desde JSON", e);
                listaProductos = new ArrayList<>();
            }
        } else {
            listaProductos = new ArrayList<>();
            Log.d("MainActivity", "No se encontró lista guardada.");
        }

        if (listaProductos.isEmpty()) {
            Log.i("MainActivity", "Lista vacía después de cargar, añadiendo datos de ejemplo.");
            addSampleDataPerCategory(); // Generar datos de ejemplo
            saveListaProductos();      // Guardar los datos de ejemplo creados para la próxima vez
        }
    }

    private void addSampleDataPerCategory() {
        listaProductos.clear();
        Resources res = getResources();
        String[] categorias = res.getStringArray(R.array.categorias);
        double precioBase = 10.0;

        for (String categoria : categorias) {
            if (!categoria.equals("Todos")) {
                String nombreProducto = "Item de " + categoria;
                double precioProducto = Math.max(0.01, precioBase);
                // Asegurar que se proporciona un valor predeterminado para la unidad
                Producto productoEjemplo = new Producto(nombreProducto, precioProducto, 1, false, categoria, "uds.");
                listaProductos.add(productoEjemplo);
                precioBase += 5.0;
            }
        }
        Log.i("MainActivity", "Datos de ejemplo por categoría añadidos. Nuevo tamaño: " + listaProductos.size());
    }

    public void AcercaDe (){
        Intent i = new Intent(this, AcercaDe.class);
        startActivity(i);
    }
}