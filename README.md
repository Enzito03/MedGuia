# MedGuia

## Visao Geral
O MedGuia e um aplicativo movel desenvolvido em Android (Jetpack Compose) com o objetivo de auxiliar pacientes com baixo letramento ou analfabetos funcionais na adesao correta aos tratamentos medicamentosos. O sistema conecta medicos e pacientes, substituindo a dependencia exclusiva de texto por identificacao visual baseada em combinacoes de cores, icones e fotos de embalagens, associadas a alarmes sonoros com recursos de confirmacao e reforco de doses.

---

## Arquitetura e Tecnologias Utilizadas
- **Linguagem**: Kotlin
- **Interface de Usuario**: Jetpack Compose (Material 3)
- **Persistencia Local**: Room Database (Versao 3 com suporte a entidades de Usuario, Alarme e Registro de Dose)
- **Gerenciamento de Alarmes e Notificacoes**: AlarmManager (alarmes exatos e retentativas), NotificationManager (notificacoes de alta prioridade com Full-Screen Intent) e BroadcastReceiver (AlarmReceiver).
- **Gerenciamento de Midia**: Carregamento de fotos de embalagens via ContentResolver e ImageBitmap.

---

## Funcionalidades Implementadas

### 1. Sistema de Autenticacao e Perfis (Medico e Paciente)
- **Login e Cadastro**: Telas dedicadas para profissionais de saude (Medicos) e Pacientes.
- **Validacao de Campos**: Validacao visual de campos obrigatorios com feedback em vermelho (isError e supportingText).

### 2. Fluxo de Vinculacao Medico-Paciente
- **Solicitacao de Vinculo**: Durante o cadastro do paciente, e possivel informar o CRM do medico responsavel, enviando uma solicitacao com status pendente.
- **Gerenciamento pelo Medico**: Na aba de pacientes do medico, solicitacoes pendentes aparecem em destaque no topo com acoes de aceitar (aprovacao imediata) ou rejeitar (com caixa de dialogo deconfirmacao).
- **Notificacoes do Medico**: O sino de notificacoes do medico indica a presenca de pacientes aguardando aprovacao de vinculo.
- **Detalhes do Paciente**: O medico pode visualizar a lista de alarmes de um paciente ativo e cadastrar novos alarmes diretamente para ele.

### 3. Configuracao de Alarmes e Validacao de Unicidade
- **Criacao de Alarmes**: Selecao de horario (TimePickerDialog), nome do remedio, seletor de icones e paleta com 8 cores de identificacao.
- **Foto da Embalagem**: Inclusao opcional de foto da caixa/embalagem do remedio.
- **Validacao de Unicidade**: O sistema impede o cadastro de um novo alarme caso ja exista outro alarme ativo com a mesma combinacao de icone e cor para o mesmo paciente.
- **Exclusao**: Opcao de exclusao de alarmes com confirmacao previa.

### 4. Linha do Tempo de Remedios (Aba Paciente)
- **Estados Visuais Dinamicos**: Os cards de medicamentos exibem a cor personalizada do alarme e se adaptam a 4 estados:
  - **Pendente**: Neutro com tonalidade da cor do remedio.
  - **Agora**: Destacado para o horario atual, com acao direta de confirmacao.
  - **Tomado**: Com opacidade reduzida e selo de confirmacao, gravando o historico na tabela de registro de doses.
  - **Perdido**: Exibido em caso de nao confirmacao apos as tentativas de reforco.

### 5. Sistema de Alarme Real (AlarmManager e Tela de Alerta)
- **Alarme em Tela Cheia**: A classe AlarmAlertActivity desperta a tela e e exibida mesmo com o dispositivo bloqueado ou com o aplicativo fechado.
- **Alarme Sonoro e Vibracao**: Reproducao do toque de alarme padrao do sistema acompanhado de vibracao continua.
- **Sistema de Soneca e Reforco**: Caso o paciente nao confirme a dose, o alarme se repete a cada 5 minutos (ate 3 tentativas). Apos a terceira tentativa sem interacao, o sistema registra automaticamente a dose como perdida no banco de dados local.
- **Confirmacao**: Ao confirmar a dose (tanto no aplicativo quanto na tela de alerta), o alarme e cancelado para o dia atual e automaticamente reprogramado para o mesmo horario no dia seguinte.
