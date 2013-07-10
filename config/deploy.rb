#Configurações do repositorio 

set :application, "Periscope"
set :deploy_name, "periscope-ear"
set :repository,  "https://github.com/UFMT/periscope.git"
set :branch, "master"

#Prompt para requisitar senha do usuário
default_run_options[:pty] = true
ssh_options[:forward_agent] = true 

task :localcopy do

	roles.clear
	server "localhost", :app
	
	#Configurações para utilizar deploy com fonte local.
	set :scm, :none
	set :repository, "."
	set :deploy_via, :copy	
	
	set :jboss_home, ENV['JBOSS_HOME'] ? ENV['JBOSS_HOME'] : "/Users/alvaroviebrantz/Downloads/jboss-as-7.1.1.Final"
	set :deploy_to, "/tmp/#{deploy_name}"
	set :use_sudo, false
	
	after "deploy","jboss:deploy"
	#after "local", "jboss:deploy"

end

task :local do

	roles.clear
	server "localhost", :app	
	
	set :jboss_home, ENV['JBOSS_HOME'] ? ENV['JBOSS_HOME'] : "/Users/alvaroviebrantz/Downloads/jboss-as-7.1.1.Final"
	set :deploy_to, "/tmp/#{deploy_name}"
	set :use_sudo, false
	
	after "deploy","jboss:deploy"
	#after "local", "jboss:deploy"

end

task :remote do

	roles.clear
	server "200.129.242.6:2204", :app
	set :user, "gaiia"	
	
  	set :maven_home, "/usr/local/apache-maven"
  	set :jboss_home, ENV['JBOSS_HOME'] ? ENV['JBOSS_HOME'] : "/usr/local/jboss-7.1.1"	
	set :deploy_to, "/tmp/#{deploy_name}"
	set :use_sudo, false
	
	after "remote", "jboss:deploy"

end

namespace :jboss do
	desc "Compile the project using maven, remove the old deploy and deploy the new compiled one."
	task :deploy do 		

		puts "==================Building with Maven======================"		
		if(exists?(:maven_home))
        	run "cd #{deploy_to}/current/ && #{maven_home}/bin/mvn clean package"
        else
        	run "cd #{deploy_to}/current/ && mvn clean package"
        end
        puts "==================Undeploy ear======================"
        run "rm -rf #{jboss_home}/standalone/deployments/#{deploy_name}.ear"
        run "rm -f #{jboss_home}/standalone/deployments/#{deploy_name}.ear.deployed"
        
        puts "==================Deploy war to JBoss======================"
        run "cp -r #{deploy_to}/current/periscope-ear/target/#{application.downcase} #{jboss_home}/standalone/deployments/#{deploy_name}.ear"
        run "touch #{jboss_home}/standalone/deployments/#{deploy_name}.ear.dodeploy"

	end

end