# java-olhovivo
A simple java wrapper for the SPTrans Olho Vivo API.

## Documentation

The **java-olhovivo** is a java spring boot wrapper for the Olho Vivo API of SPTrans v2.1

To authenticate to the Olho Vivo API service you must make a previous call using the http POST method informing your access token. This call will return true when authentication succeeds and false in case of errors.

```java
public class OlhoVivoApi {

	final String BASE_URL = "http://api.olhovivo.sptrans.com.br/v2.1/";
	
	RestTemplate template = new RestTemplate();
	HttpEntity<String> entity;
	
	/**
	 * Para autenticar-se no serviço de API do Olho Vivo é necessário efetuar uma chamada prévia utilizando o método http POST informando seu token de acesso. 
	 * Essa chamada irá retornar true quando a autenticação for realizada com sucesso e false em caso de erros.
	 * @param  chave de acesso
	 */
	public void login(String token) {
        
		String url = BASE_URL + "Login/Autenticar?token=" + token;
		
        ResponseEntity<String> forEntity = template.postForEntity(url, HttpMethod.POST, String.class);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie",forEntity.getHeaders().get("Set-Cookie").stream().collect(Collectors.joining(";")));
        this.entity = new HttpEntity<String>(headers);
		
	}

	/**
	 * Realiza uma busca das linhas do sistema com base no parâmetro informado. 
	 * Se a linha não é encontrada então é realizada uma busca fonetizada na denominação das linhas.
	 * @param Aceita denominação ou número da linha (total ou parcial). Exemplo: 8000, Lapa ou Ramos
	 * @return
	 */
	
	public Linha[] getLinhas(String param) {
		
		String url = BASE_URL + "Linha/Buscar?termosBusca=" + param;
		
		ResponseEntity<Linha[]> response = template.exchange(url, HttpMethod.GET, entity, Linha[].class);
		
		return response.getBody();
		
	}
	
	/**
	 * Realiza uma busca fonética das paradas de ônibus do sistema com base no parâmetro informado. 
	 * A consulta é realizada no nome da parada e também no seu endereço de localização.
	 * @param Aceita nome da parada ou endereço de localização (total ou parcial). Exemplo: Afonso, ou Balthazar da Veiga
	 * @return
	 */
	public Parada[] getParadas(String param) {
		
		String url = BASE_URL + "Parada/Buscar?termosBusca=" + param;
		
		ResponseEntity<Parada[]> response = template.exchange(url, HttpMethod.GET, entity, Parada[].class);
		
		return response.getBody();
		
	}
	
	/**
	 * Retorna uma lista completa com a última localização de todos os veículos mapeados com suas devidas posições lat / long
	 * @return
	 */
	public Posicao getPosicoes(String param) {
		
		String url = BASE_URL + "Posicao";
		
		ResponseEntity<Posicao> response = template.exchange(url, HttpMethod.GET, entity, Posicao.class);
		
		return response.getBody();
		
	}
	
}
```

Full documentation: http://bit.ly/2v4GxTv
