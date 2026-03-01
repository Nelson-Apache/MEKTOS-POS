package com.nap.pos.application.dto.importacion;

/**
 * Par categoría + subcategoría que aparece en el archivo de importación
 * pero no existe en la base de datos.
 * nombreCategoria puede ser vacío si el archivo no tiene columna de categoría.
 */
public record CategoriaSubcategoriaNueva(String nombreCategoria, String nombreSubcategoria) {}
