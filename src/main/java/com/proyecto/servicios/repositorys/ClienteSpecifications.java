package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.dto.ClienteFiltroRequestDTO;
import com.proyecto.servicios.entity.Cliente;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Especificaciones dinámicas JPA Criteria API para la búsqueda combinable de clientes.
 * Garantiza que las consultas normales excluyan siempre a los usuarios eliminados lógicamente
 * y construye predicados parametrizados evitando la inyección SQL.
 */
public final class ClienteSpecifications {

    private ClienteSpecifications() {
    }

    /**
     * Sobrecarga 1: Construye la especificación JPA a partir del DTO completo de filtros combinables.
     *
     * @param filtro DTO con los criterios opcionales enviados por el cliente
     * @return Specification para Spring Data JPA
     */
    public static Specification<Cliente> construirCriterios(ClienteFiltroRequestDTO filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Regla obligatoria: Excluir usuarios eliminados lógicamente
            predicates.add(cb.isTrue(root.get("activo")));

            if (filtro == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            // 2. Filtro por nombre (búsqueda parcial insensible a mayúsculas en nombre y apellidos)
            String nombre = normalizar(filtro.getNombre());
            if (nombre != null) {
                String pattern = "%" + nombre.toLowerCase(Locale.ROOT) + "%";
                Predicate pNombre = cb.like(cb.lower(root.get("nombre")), pattern);
                Predicate pSegundoNombre = cb.like(cb.lower(root.get("segundoNombre")), pattern);
                Predicate pApellidoP = cb.like(cb.lower(root.get("apellidoPaterno")), pattern);
                Predicate pApellidoM = cb.like(cb.lower(root.get("apellidoMaterno")), pattern);
                predicates.add(cb.or(pNombre, pSegundoNombre, pApellidoP, pApellidoM));
            }

            // 3. Filtro por CURP (coincidencia exacta)
            String curp = normalizar(filtro.getCurp());
            if (curp != null) {
                predicates.add(cb.equal(cb.upper(root.get("curp")), curp.toUpperCase(Locale.ROOT)));
            }

            // 4. Filtro por RFC (coincidencia exacta)
            String rfc = normalizar(filtro.getRfc());
            if (rfc != null) {
                predicates.add(cb.equal(cb.upper(root.get("rfc")), rfc.toUpperCase(Locale.ROOT)));
            }

            // 5. Filtro por Correo Electrónico (coincidencia exacta insensible a mayúsculas)
            String correo = normalizar(filtro.getCorreo());
            if (correo != null) {
                predicates.add(cb.equal(cb.lower(root.get("correo")), correo.toLowerCase(Locale.ROOT)));
            }

            // 6. Filtro por rango de fechas de creación
            if (filtro.getFechaCreacionDesde() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), filtro.getFechaCreacionDesde()));
            }
            if (filtro.getFechaCreacionHasta() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), filtro.getFechaCreacionHasta()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Sobrecarga 2: Construye la especificación JPA a partir de los atributos clave nombre y correo.
     * Reutiliza la lógica del método principal sin duplicar predicados.
     *
     * @param nombre Filtro parcial de nombre
     * @param correo Filtro exacto de correo
     * @return Specification combinada
     */
    public static Specification<Cliente> construirCriterios(String nombre, String correo) {
        ClienteFiltroRequestDTO dto = ClienteFiltroRequestDTO.builder()
                .nombre(nombre)
                .correo(correo)
                .build();
        return construirCriterios(dto);
    }

    /**
     * Método auxiliar reutilizable para sanear y normalizar cadenas de búsqueda.
     *
     * @param valor Cadena de entrada
     * @return Cadena recortada o null si era vacía o nula
     */
    public static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}
