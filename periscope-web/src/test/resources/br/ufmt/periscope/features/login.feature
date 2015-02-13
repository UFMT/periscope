Feature: Login

@foo 	
  Scenario Outline: Login com/sem Sucesso
    Given Eu estou na pagina "http://localhost:8080/periscope/login.jsf"
    When Eu preencho o campo usuario com "<username>"
    And Eu preencho o campo senha com "<password>"
    And Eu clicar no botao "Login"
	Then Eu deveria estar na pagina "<pagina>"
    And Eu deveria receber uma mensagem "<mensagem>"


	Examples:
		| username |   password 	  |  pagina      |  mensagem                   | 
		| andrerox |   andrerox1202   |  projectList |  Bem vindo ao Periscope     |
		| andrerox |   andrerox	      |  login       |  Usuário/Senha inválidos.   |
		| andre	   |   andrerox1202   |  login       |  Usuário/Senha inválidos.   |
		| andre	   |   andrerox       |  login       |  Usuário/Senha inválidos.   |


 	
 	
