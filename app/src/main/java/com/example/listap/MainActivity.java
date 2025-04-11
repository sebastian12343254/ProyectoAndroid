package com.example.listap;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources; // Necesaria para obtener recursos
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils; // Para verificar strings vacíos
import android.text.TextWatcher;
import android.util.Log; // Para logs de depuración
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.Locale; // Para formateo de moneda/números
import java.util.stream.Collectors; // Para usar Streams (requiere API 24+)

public class MainActivity extends AppCompatActivity implements ProductoAdapterListener {

    // Constantes para SharedPreferences
    private static final String PREFS_NAME = "ListaPrefs";
    private static final String KEY_LISTA_PRODUCTOS = "listaProductos";

    // Componentes UI
    private RecyclerView recyclerView;
    private Button btnVerAñadidos, btnVerNoAñadidos, btnVerTodos, btnAgregar;
    private TextView totalText;
    private Spinner spinnerOrdenar, spinnerCategoria;
    private EditText searchBar;

    // Datos y Lógica
    private ArrayList<Producto> listaProductos = new ArrayList<>(); // Lista maestra
    private ProductoAdapter adapter;
    private Gson gson = new Gson();

    // Estado actual de los filtros y orden
    private String categoriaSeleccionada = "Todos";
    private String filtroBusqueda = "";
    private EstadoFiltro estadoFiltro = EstadoFiltro.TODOS;
    private int ordenSeleccionado = 0; // Índice del spinner de orden

    // Enum para el filtro de estado
    private enum EstadoFiltro { TODOS, ANADIDOS, NO_ANADIDOS }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Cargar datos guardados O generar datos de ejemplo si es necesario
        loadListaProductos();

        // Inicializar Vistas
        initializeUIComponents();

        // Configurar RecyclerView y Adapter
        setupRecyclerView();

        // Configurar Spinners (Categoría y Ordenar)
        setupSpinners();

        // Configurar Barra de Búsqueda
        setupSearchBar();

        // Configurar Botones de Filtro de Estado
        setupFiltroEstadoButtons();

        // Configurar Botón Agregar
        btnAgregar.setOnClickListener(v -> mostrarDialogoAgregarOEditar(null)); // null para modo agregar

        // Aplicar filtros iniciales y mostrar lista
        aplicarFiltrosYSort();
        actualizarTotal(); // Calcular total inicial
    }

    // --- Inicialización y Configuración de UI ---

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
        // Pasamos 'this' porque MainActivity implementa ProductoAdapterListener
        // Iniciamos el adapter con una lista vacía; se poblará con aplicarFiltrosYSort()
        adapter = new ProductoAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSpinners() {
        // Spinner Categorías
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

        // Spinner Ordenar
        ArrayAdapter<CharSequence> adapterOrdenar = ArrayAdapter.createFromResource(this,
                R.array.opciones_ordenar, android.R.layout.simple_spinner_item);
        adapterOrdenar.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrdenar.setAdapter(adapterOrdenar);
        spinnerOrdenar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                ordenSeleccionado = position;
                aplicarFiltrosYSort(); // Reaplicar filtros y orden
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
                aplicarFiltrosYSort(); // Volver a filtrar y ordenar
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

    // --- Lógica Principal de Filtrado y Ordenamiento ---

    private void aplicarFiltrosYSort() {
        // 1. Empezar con la lista maestra completa
        List<Producto> listaFiltrada = new ArrayList<>(listaProductos);

        // 2. Filtrar por Categoría (si no es "Todos")
        if (!"Todos".equals(categoriaSeleccionada)) {
            listaFiltrada = listaFiltrada.stream()
                    .filter(p -> p.getCategoria().equals(categoriaSeleccionada))
                    .collect(Collectors.toList());
        }

        // 3. Filtrar por Búsqueda (si hay texto)
        if (!filtroBusqueda.isEmpty()) {
            listaFiltrada = listaFiltrada.stream()
                    .filter(p -> p.getNombre().toLowerCase().contains(filtroBusqueda))
                    .collect(Collectors.toList());
        }

        // 4. Filtrar por Estado (Añadido/No Añadido/Todos) ANTES de ordenar visualmente
        if (estadoFiltro != EstadoFiltro.TODOS) {
            boolean buscarAnadidos = (estadoFiltro == EstadoFiltro.ANADIDOS);
            listaFiltrada = listaFiltrada.stream()
                    .filter(p -> p.isAñadido() == buscarAnadidos)
                    .collect(Collectors.toList());
        }

        // 5. Ordenar la lista resultante
        ordenarLista(listaFiltrada);

        // 6. Actualizar el Adapter con la lista final filtrada y ordenada
        adapter.submitList(listaFiltrada);
    }

    private void ordenarLista(List<Producto> lista) {
        Comparator<Producto> comparator;
        switch (ordenSeleccionado) {
            case 1: // Precio Ascendente
                comparator = Comparator.comparingDouble(Producto::getPrecio);
                break;
            case 2: // Nombre Descendente
                comparator = (p1, p2) -> p2.getNombre().compareToIgnoreCase(p1.getNombre());
                break;
            case 3: // Precio Descendente
                comparator = (p1, p2) -> Double.compare(p2.getPrecio(), p1.getPrecio());
                break;
            case 0: // Nombre Ascendente (por defecto)
            default:
                comparator = Comparator.comparing(Producto::getNombre, String.CASE_INSENSITIVE_ORDER);
                break;
        }
        // Usamos sort directamente sobre la lista filtrada antes de pasarla al adapter
        Collections.sort(lista, comparator);
    }


    // --- Implementación de ProductoAdapterListener (Callbacks desde el Adapter) ---

    @Override
    public void onProductoEstadoCambiado(Producto producto, boolean isChecked) {
        Producto productoEnLista = findProductoInMasterList(producto);
        if (productoEnLista != null && productoEnLista.isAñadido() != isChecked) {
            productoEnLista.setAñadido(isChecked);
            saveListaProductos(); // Guardar cambio
            actualizarTotal(); // Recalcular total basado en la lista maestra
            // Si el filtro de estado está activo, necesitamos refiltrar la vista
            if (estadoFiltro != EstadoFiltro.TODOS) {
                aplicarFiltrosYSort();
            }
            // No es necesario notificar al adapter aquí, el checkbox ya cambió visualmente.
            // Si el filtro estaba activo, aplicarFiltrosYSort lo hará.
        }
    }

    @Override
    public void onProductoCantidadCambiada(Producto producto, int nuevaCantidad) {
        Producto productoEnLista = findProductoInMasterList(producto);
        if (productoEnLista != null && productoEnLista.getCantidad() != nuevaCantidad) {
            productoEnLista.setCantidad(nuevaCantidad);
            saveListaProductos(); // Guardar cambio
            // Recalcular total solo si el producto afectado estaba añadido
            if (productoEnLista.isAñadido()) {
                actualizarTotal();
            }
            // No es necesario actualizar el adapter, el EditText ya cambió.
        }
    }

    @Override
    public void onProductoEliminar(final Producto producto) {
        // Usamos final producto para acceder dentro del lambda
        new AlertDialog.Builder(this)
                .setTitle("Confirmar Eliminación")
                .setMessage("¿Seguro que quieres eliminar '" + producto.getNombre() + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    boolean removed = listaProductos.remove(producto); // Usa equals()
                    if (removed) {
                        saveListaProductos();    // Guardar cambios
                        aplicarFiltrosYSort(); // Actualizar la vista
                        actualizarTotal();     // Actualizar el total
                    } else {
                        Log.w("MainActivity", "Producto a eliminar no encontrado en lista maestra: " + producto.getNombre());
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onProductoEditar(Producto producto) {
        // Buscar la instancia real en la lista maestra para editarla
        Producto productoParaEditar = findProductoInMasterList(producto);
        if (productoParaEditar != null) {
            mostrarDialogoAgregarOEditar(productoParaEditar);
        } else {
            Log.e("MainActivity", "No se encontró el producto a editar en la lista maestra: " + producto.getNombre());
            Toast.makeText(this, "Error al intentar editar el producto.", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper para encontrar el producto en la lista maestra (usa equals definido en Producto)
    private Producto findProductoInMasterList(Producto productoToFind) {
        for (Producto p : listaProductos) {
            if (p.equals(productoToFind)) {
                return p;
            }
        }
        return null;
    }


    // --- Diálogo Agregar/Editar Producto ---

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

        // Configurar Spinner de categorías en el diálogo (sin la opción "Todos")
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

        // Si estamos editando, llenar los campos con los datos existentes
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
                        inputLayoutNombre.setError(null); // Limpiar error si existe
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
                        return; // Detener si hay errores
                    }

                    if (isEditing) {
                        productoAEditar.setNombre(nombre);
                        productoAEditar.setPrecio(precio);
                        productoAEditar.setCategoria(categoriaSeleccionadaDialogo);
                        productoAEditar.setUnidad(unidad);
                    } else {
                        int cantidadInicial = 1;
                        Producto nuevo = new Producto(nombre, precio, cantidadInicial, false, categoriaSeleccionadaDialogo, unidad);
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
        // Iterar SIEMPRE sobre la lista maestra completa para calcular el total
        for (Producto p : listaProductos) {
            if (p.isAñadido()) {
                // Asegurarse que la cantidad sea no negativa
                total += p.getPrecio() * Math.max(0, p.getCantidad());
            }
        }
        // Formatear el total a dos decimales usando el Locale adecuado
        totalText.setText(String.format(Locale.getDefault(), "Total: $%.2f", total));
    }


    // --- Persistencia con SharedPreferences y Gson ---

    private void saveListaProductos() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        try {
            String jsonLista = gson.toJson(listaProductos); // Convertir lista a JSON
            editor.putString(KEY_LISTA_PRODUCTOS, jsonLista);
            editor.apply(); // Guardar asíncronamente
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
                listaProductos = gson.fromJson(jsonLista, type); // Convertir JSON a lista
                // Verificación adicional por si el JSON guardado era inválido o representaba null
                if (listaProductos == null) {
                    listaProductos = new ArrayList<>();
                    Log.w("MainActivity", "El JSON cargado resultó en una lista nula, iniciando lista vacía.");
                }
                Log.d("MainActivity", "Lista cargada desde SharedPreferences. Tamaño: " + listaProductos.size());
            } catch (Exception e) {
                // Si hay un error al deserializar (JSON corrupto, cambio de clase Producto), empezar con una lista vacía
                Log.e("MainActivity", "Error al cargar/deserializar lista desde JSON", e);
                listaProductos = new ArrayList<>();
            }
        } else {
            // No hay nada guardado, iniciar lista vacía
            listaProductos = new ArrayList<>();
            Log.d("MainActivity", "No se encontró lista guardada.");
        }

        // Si después de intentar cargar, la lista está vacía, añadir datos de ejemplo por categoría
        if (listaProductos.isEmpty()) {
            Log.i("MainActivity", "Lista vacía después de cargar, añadiendo datos de ejemplo.");
            addSampleDataPerCategory(); // Generar datos de ejemplo
            saveListaProductos();      // Guardar los datos de ejemplo creados para la próxima vez
        }
    }

    // Método para añadir un producto de ejemplo por cada categoría definida en R.array.categorias
    private void addSampleDataPerCategory() {
        listaProductos.clear(); // Asegurarse de empezar limpio
        Resources res = getResources();
        String[] categorias = res.getStringArray(R.array.categorias);
        double precioBase = 10.0; // Precio inicial de ejemplo

        for (String categoria : categorias) {
            // Omitir la categoría "Todos"
            if (!categoria.equals("Todos")) {
                String nombreProducto = "Item de " + categoria; // Nombre genérico
                // Asegurar precio positivo y con formato US para consistencia interna
                double precioProducto = Math.max(0.01, precioBase); // Precio mínimo 0.01
                Producto productoEjemplo = new Producto(nombreProducto, precioProducto, 1, false, categoria);
                listaProductos.add(productoEjemplo);
                precioBase += 5.0; // Incrementar el precio para el siguiente item (opcional)
            }
        }
        Log.i("MainActivity", "Datos de ejemplo por categoría añadidos. Nuevo tamaño: " + listaProductos.size());
    }
}