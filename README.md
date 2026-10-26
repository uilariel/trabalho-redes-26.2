# 🎮 Steam Analyzer

Aplicação CLI desenvolvida em Java para recolha e análise em tempo real dos produtos e jogos mais vendidos da plataforma **Steam**, integrando dados de preços regionais, categorias/géneros e métricas de avaliações dos utilizadores através de requisições HTTP REST[cite: 2].

Projeto desenvolvido para a disciplina de **Redes de Computadores** da Universidade Federal do Rio Grande do Norte (**UFRN**).

---

## Funcionalidades

- **Consulta Regional de Vendas de fácil expansão**: Suporte a múltiplas regiões de mercado (Brasil, Estados Unidos, Japão, Alemanha/Europa e Reino Unido) com formatação monetária nativa (`R$`, `US$`, `¥`, `€`, `£`)[cite: 2].
- **Consumo de APIs REST da Steam**[cite: 2]:
  - `featuredcategories`: Recolha dos produtos do ranking *Top Sellers* da região selecionada[cite: 2].
  - `appdetails`: Extração detalhada de géneros e metadados de cada aplicação[cite: 2].
  - `appreviews`: Análise quantitativa de avaliações (total de análises, avaliações positivas e percentagem de aprovação)[cite: 2].
- **Análise de Dados e Métricas**:
  - Formatação em tabela de ranking no terminal com alinhamento dinâmico[cite: 2].
  - Colorização via códigos de escape ANSI conforme a percentagem de aprovação dos jogos[cite: 2].
  - Gráfico em barras ASCII para distribuição de frequência dos géneros no Top 10[cite: 2].
  - Cronometragem em milissegundos da recolha e reutilização de conexão[cite: 2].

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem**: Java 17+ (utilizando *Java Records*, *Switch Expressions* e *HttpClient* nativo)
- **Gerenciador de Build**: Apache Maven
- **Cliente HTTP**: `java.net.http.HttpClient` (suporte nativo a HTTP/2)
- **Manipulação de JSON**: [Google Gson 2.10+](https://github.com/google/gson)

---

## Como Instalar e Executar

### 1. Instalar o Apache Maven

Caso ainda não tenha o Maven instalado na sua máquina.

---

### 2. Compilar e Executar o Projeto

1. **Clone o repositório e acesse a pasta do projeto:**

   ```bash
   git clone https://github.com/seu-usuario/trabalho-redes-26.2.git
   cd trabalho-redes-26.2
   ```

2. **Compile o projeto e baixe as dependências:**

   ```bash
   mvn clean compile
   ```

3. **Execute a aplicação:**

   * **Via Maven:**

     ```bash
     mvn exec:java -Dexec.mainClass="br.ufrn.redes.Main"
     ```

   * **Ou via Java nativo:**

     ```bash
     java -cp "target/classes:$(mvn dependency:build-classpath | grep -v '\[INFO\]' | tr '\n' ':')" br.ufrn.redes.Main
     ```

## 👥 Desenvolvedores
Equipe:

* Lucas Lopes - [perfil GitHub](https://github.com/lucas1noid)
* Miguel Guion - [perfil GitHub](https://github.com/Migalhaz)
* William Ariel - [perfil GitHub](https://github.com/uilariel)

