# 🧩 GuessMe API

API REST desenvolvida em **Java com Spring Boot** para gerenciar a lógica do jogo **GuessMe**, um jogo de adivinhação de personagens com apoio de inteligência artificial.  
A aplicação recebe perguntas do usuário, controla o fluxo da partida, inicia jogos por categoria, gera dicas e retorna respostas consumidas pelo frontend web.

---

## 📌 Visão Geral

A **GuessMe API** é o backend do ecossistema GuessMe e foi construída para fornecer a lógica central do jogo, permitindo:

- iniciar novas partidas
- escolher categorias para o personagem secreto
- receber perguntas do jogador
- responder com apoio de IA generativa
- gerar dicas contextuais
- retornar dados do personagem quando o jogador acerta
- integrar com o frontend web do projeto

---

## 🧱 Tecnologias Utilizadas

- **Java 21**
- **Spring Boot 3.5.7**
- **Spring WebFlux**
- **Maven**
- **Lombok**
- **Google Gemini API**
- **WebClient**

---

## 🏛️ Estrutura do Projeto

A aplicação foi organizada em camadas simples para separar responsabilidades e facilitar a evolução do projeto.

### Estrutura principal

- `controller` — endpoints REST do jogo
- `service` — regras de negócio e comunicação com IA
- `dto` — objetos de transferência de dados
- `config` — configuração do Gemini e do `WebClient`
- `resources` — propriedades da aplicação

---

## 🚀 Funcionalidades

- Início de partida com categoria opcional
- Suporte a categorias como **Geral**, **Anime**, **Games**, **Filmes**, **Séries** e **Quadrinhos**
- Envio de perguntas para a IA
- Respostas curtas e consistentes durante a partida
- Geração de dicas contextuais
- Detecção de acerto do personagem
- Retorno de dados do personagem descoberto
- Busca de imagem para exibição no frontend
- Integração com o frontend web do GuessMe

---

## 🤖 Integração com IA

O projeto já está integrado ao **Gemini**, utilizando `WebClient` para chamadas à API do Google.  
A chave é carregada por configuração externa via `gemini.properties`, e a aplicação também utiliza `google.properties` nas configurações do projeto.

---

## 🔗 Endpoints

### Jogo

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/game/categories` | Lista as categorias disponíveis |
| GET | `/api/game/start` | Inicia uma nova partida com categoria opcional via query param |
| POST | `/api/game/start` | Inicia uma nova partida via body JSON |
| POST | `/api/game/ask` | Envia uma pergunta para a IA |
| POST | `/api/game/hint` | Gera uma dica para o jogador |

---

## 📥 Exemplos de Requisição

### Listar categorias

**GET** `/api/game/categories`

### Iniciar partida sem categoria

**GET** `/api/game/start`

### Iniciar partida com categoria

**GET** `/api/game/start?category=Anime`

### Iniciar partida via JSON

**POST** `/api/game/start`

```json
{
  "category": "Games"
}
```

### Enviar pergunta

**POST** `/api/game/ask`

```json
{
  "question": "Esse personagem é humano?",
  "sessionId": "<id retornado pelo start>"
}
```

### Pedir dica

**POST** `/api/game/hint`

```json
{
  "sessionId": "<id retornado pelo start>"
}
```

---

## 🔑 Fluxo de Sessão (importante para o frontend)

Cada partida possui um `sessionId` único gerado pelo backend. O frontend **deve** armazená-lo e enviá-lo em todas as requisições subsequentes.

### 1. Iniciar partida → guardar o `sessionId`

**POST** `/api/game/start`

```json
{ "category": "Anime" }
```

Resposta:

```json
{
  "answer": "Ok! Já escolhi um personagem da categoria: Anime. Pode fazer sua primeira pergunta!",
  "success": false,
  "character": null,
  "sessionId": "3f2a1b4c-5d6e-7f8a-9b0c-1d2e3f4a5b6c"
}
```

### 2. Perguntar → enviar o `sessionId`

**POST** `/api/game/ask`

```json
{
  "question": "Esse personagem é humano?",
  "sessionId": "3f2a1b4c-5d6e-7f8a-9b0c-1d2e3f4a5b6c"
}
```

Resposta:

```json
{
  "answer": "Sim",
  "success": false,
  "character": null,
  "sessionId": "3f2a1b4c-5d6e-7f8a-9b0c-1d2e3f4a5b6c"
}
```

### 3. Pedir dica → enviar o `sessionId`

**POST** `/api/game/hint`

```json
{
  "sessionId": "3f2a1b4c-5d6e-7f8a-9b0c-1d2e3f4a5b6c"
}
```

Resposta:

```json
{
  "answer": "Este personagem é conhecido por sua habilidade com espadas.",
  "success": false,
  "character": null,
  "sessionId": "3f2a1b4c-5d6e-7f8a-9b0c-1d2e3f4a5b6c"
}
```

### 4. Resposta de vitória

Quando o jogador acerta, `success` é `true` e `character` contém os dados do personagem:

```json
{
  "answer": "Sim! O personagem é Naruto Uzumaki.\nObra: Naruto",
  "success": true,
  "character": {
    "name": "Naruto Uzumaki",
    "work": "Naruto",
    "image": "https://upload.wikimedia.org/..."
  },
  "sessionId": "3f2a1b4c-5d6e-7f8a-9b0c-1d2e3f4a5b6c"
}
```

> **Nota:** Omitir `sessionId` nas requisições de `/ask` e `/hint` ainda funciona para testes locais (usa uma sessão padrão compartilhada), mas **não é o comportamento recomendado** para o frontend em produção. Cada partida deve usar o `sessionId` retornado pelo `/start`.

---

## ⚙️ Como Executar Localmente

### Pré-requisitos

* **Java 21** (JDK)
* **Maven 3.9+**

### 1. Clone o repositório

```bash
git clone https://github.com/Daniel-Macedo-dev/guessme-api.git
cd guessme-api/guessme
```

### 2. Configure as credenciais (obrigatório: Gemini / opcional: Google Image Search)

As chaves de API **não são commitadas**. Você tem duas opções equivalentes:

#### Opção A — arquivos `.properties` locais (recomendado para desenvolvimento)

```bash
# dentro de guessme/src/main/resources/
cp gemini.properties.example gemini.properties
cp google.properties.example google.properties
```

Edite os arquivos criados e substitua os placeholders pelas suas chaves reais.

#### Opção B — variáveis de ambiente

| Variável             | Propriedade Spring   | Obrigatório? |
|----------------------|----------------------|--------------|
| `GEMINI_API_KEY`     | `gemini.api.key`     | Sim          |
| `GEMINI_MODEL`       | `gemini.model`       | Não (padrão: `gemini-3.1-flash-lite`) |
| `GOOGLE_API_KEY`     | `google.api.key`     | Não          |
| `GOOGLE_SEARCH_CX`   | `google.search.cx`   | Não          |

**PowerShell (Windows):**

```powershell
$env:GEMINI_API_KEY   = "sua-chave-gemini"
$env:GOOGLE_API_KEY   = "sua-chave-google"   # opcional
$env:GOOGLE_SEARCH_CX = "seu-cx"             # opcional
mvn spring-boot:run
```

**Bash (Linux/macOS):**

```bash
export GEMINI_API_KEY="sua-chave-gemini"
export GOOGLE_API_KEY="sua-chave-google"   # opcional
export GOOGLE_SEARCH_CX="seu-cx"           # opcional
mvn spring-boot:run
```

> **Sem Gemini:** O servidor inicia, mas perguntas e dicas retornam uma mensagem de erro amigável.  
> **Sem Google Image Search:** O jogo funciona normalmente; o campo `character.image` retorna vazio quando o jogador acerta.

### 3. Execute o projeto

```bash
mvn spring-boot:run
```

### 4. A aplicação estará disponível em

```text
http://localhost:8080
```

### 5. Executar testes (sem credenciais reais)

```bash
mvn test
```

Os testes são completamente locais — nenhuma chamada a Gemini ou Google é feita.  
Para rodar o teste de integração ao vivo (requer credenciais reais):

```bash
mvn test -Dgroups=live -Dsurefire.excludedGroups=""
```

### 6. CORS

Por padrão, o servidor aceita requisições de `http://localhost:5173` (Vite dev server).  
Para adicionar origens adicionais, edite `application.properties`:

```properties
cors.allowed-origins=http://localhost:5173,https://seu-dominio.com
```

Ou defina a variável `CORS_ALLOWED_ORIGINS`.

---

## 🧪 Estado Atual do Projeto

* ✅ Backend funcional com endpoints principais
* ✅ Integração com Gemini implementada
* ✅ Suporte a categorias
* ✅ Geração de dicas
* ✅ Retorno estruturado com DTOs
* ✅ Busca de imagem para exibição no frontend
* ✅ Gerenciamento de sessão por `sessionId`
* ✅ Testes locais e determinísticos (`mvn test` não requer credenciais)
* ✅ CORS centralizado e configurável

---

## 🎯 Objetivos do Projeto

Este projeto foi desenvolvido com foco em:

* prática de backend com Java e Spring Boot
* integração com IA generativa
* uso de WebFlux e `WebClient`
* construção de API para jogo interativo
* integração entre backend e frontend
* composição de portfólio com projeto de IA aplicada

---

## 🔗 Projeto Relacionado

Frontend do ecossistema:

* **GuessMe** — interface web responsável pela interação do usuário com a API

---

## 📄 Licença

Este projeto está licenciado sob a **MIT License**.
