# 🚌 BusConnect Backend – Flujo de Trabajo Git

Este repositorio sigue una **arquitectura de microservicios** organizada con ramas dedicadas para cada etapa del desarrollo.  
El objetivo es mantener un flujo claro, ordenado y colaborativo entre todas las personas del equipo.

---

## 🌿 Estructura de Ramas

| Rama | Propósito | Quién la toca |
|------|------------|---------------|
| **`main`** | Rama principal y estable. Contiene solo código probado y listo para producción. | Solo mantenedoras. |
| **`develop`** | Rama de integración. Aquí se fusionan microservicios revisados y validados. | Solo Irina o responsables de revisión. |
| **`review-hub`** | Rama de revisión colaborativa. Todas las PR se hacen hacia aquí. | Todo el equipo. |
| **`feature/*`** | Ramas individuales para desarrollar cada microservicio o funcionalidad. | Cada desarrolladora. |

---

## ⚙️ Flujo de trabajo

### 1️⃣ Crear una nueva feature
Cada microservicio o tarea importante se desarrolla en una rama propia:

```bash
git checkout review-hub
git pull origin review-hub
git checkout -b feature/<nombre-del-servicio>
```

Ejemplo:

```bash
git checkout -b feature/catalog-service
```

### 2️⃣ Subir cambios

Cuando termines una parte del microservicio:

```bash
git add .
git commit -m "feat: implement initial endpoints for catalog-service"
git push origin feature/catalog-service
```

### 3️⃣ Crear Pull Request (PR)

En GitHub, abre una PR hacia **`review-hub`**, no hacia `develop` ni `main`.  
Ahí se revisa el código, se comentan mejoras y se aprueba cuando esté estable.

### 4️⃣ Revisión en equipo

- Las PR se revisan dentro de **`review-hub`**.
- Si todo está correcto, se hace merge manualmente hacia **`develop`**.
- `develop` debe reflejar una versión funcional con todos los servicios integrados.

### 5️⃣ Merge a develop

Solo la responsable técnica (Irina o quien se designe) puede aprobar el merge:

```bash
git checkout develop
git pull origin develop
git merge review-hub
git push origin develop
```

### 6️⃣ Merge a main

Cuando todo está validado y listo para producción:

```bash
git checkout main
git pull origin main
git merge develop
git push origin main
```

---

## 🧩 Convenciones de nombres

| Tipo de rama | Formato | Ejemplo |
|--------------|---------|---------|
| Feature | `feature/<nombre>` | `feature/catalog-service` |
| Bugfix | `fix/<nombre>` | `fix/docker-compose-path` |
| Hotfix | `hotfix/<nombre>` | `hotfix/login-endpoint-error` |

---

## 🧱 Reglas básicas

✅ Nunca trabajar directamente sobre `develop` o `main`.  
✅ Siempre hacer Pull Request hacia **`review-hub`**.  
✅ Mantener commits claros y descriptivos.  
✅ Antes de hacer merge, probar el servicio localmente o con Docker.  
✅ Si algo rompe `develop`, se revierte y se reabre la PR.

---

## 💬 Ejemplo de flujo visual

```
(feature/*) ---> review-hub ---> develop ---> main
      ^              ^              ^          ^
      |              |              |          |
   código      revisión         integración   producción
```

---

## 💜 Cultura de equipo

El objetivo de este flujo no es solo técnico:  
busca fomentar **colaboración, aprendizaje y calidad** en el código.  
Cada PR es una oportunidad de mejorar juntas.
