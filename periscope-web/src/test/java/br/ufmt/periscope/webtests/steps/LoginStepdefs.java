package br.ufmt.periscope.webtests.steps;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class LoginStepdefs {
	private WebDriver driver;
	private boolean login = false;
	
	@Given("^Eu estou na pagina \"([^\"]*)\"$")
	public void Eu_estou_na_pagina(String site) throws Throwable {
        System.setProperty("webdriver.chrome.driver", "/Users/andre/Downloads/chromedriver");
		driver = new ChromeDriver();
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
		driver.get(site);
	}

	@When("^Eu preencho o campo usuario com \"([^\"]*)\"$")
	public void Eu_preencho_o_campo_usuario_com(String username)
			throws Throwable {
		driver.findElement(By.id("username")).sendKeys(username);
	}

	@When("^Eu preencho o campo senha com \"([^\"]*)\"$")
	public void Eu_preencho_o_campo_senha_com(String password) throws Throwable {
		driver.findElement(By.id("password")).sendKeys(password);
	}

	@When("^Eu clicar no botao \"([^\"]*)\"$")
	public void Eu_clicar_no_botao(String btnId) throws Throwable {
		driver.findElement(By.id(btnId.toLowerCase())).click();
	}

	@Then("^Eu deveria estar na pagina \"([^\"]*)\"$")
	public void Eu_deveria_estar_na_pagina(String page) throws Throwable {	
		assertTrue("Pagina incorreta ", driver.getCurrentUrl().endsWith(page+".jsf"));
		if(page.contentEquals("login")){			
			login = false;
		}else{
			login = true;
		}
	}

	@Then("^Eu deveria receber uma mensagem \"([^\"]*)\"$")
	public void Ela_deveria_responder_com(String mensagem) throws Throwable {
		if(login){
			try {
				assertTrue("Mensagem de login incorreta ", driver.getPageSource()
						.contains(mensagem));
			} finally {
				driver.quit();
			}
		}else{
			try {
				assertTrue("Mensagem em caso de falha de login esta incorreta ", driver.getPageSource()
						.contains(mensagem));	
			} finally {
				driver.quit();
			}
		}
	}
	
	
}
