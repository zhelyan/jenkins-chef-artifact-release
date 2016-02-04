# chef-wrapper

Jenkins plugin adding some basic integration with Chef.
 
## Prerequisites

- Needs the chef-api plugin ( https://github.com/zhelyan/jenkins-chef-api ) (The Jenkins Chef server client should have admin privileges.)
- Token macro plugin
- Tested against Chef server 11 only

## Note

- I am not planning to actively develop this plugin anymore
- The functionality provided by this plugin should be weeded out into separate plugins. 
- Pull requests are welcomed and I will do my best to maintain the plugin but might have to source other maintainers at some later stage. 

## Installation

* Build: `mvn package`
* Go to `<JENKINS>/pluginManager/advanced` and upload `<THIS_REPO>/target/chef-wrapper.hpi`


## This plugin provides:

### Build parameter: List of the Chef environments

  You can optionally exclude environments from appearing in the dropdown.
  
### Build wrapper - Saves details about your build artifacts into a Chef data bag item.

   There are pros and cons for why you should or shouldn't use Chef to deploy your applications. The build wrapper will save information about the build artifacts produced by your Jenkins job to a Chef data bag item. 
   Your application deployment cookbook can then use this information to deploy the specified artifacts.
   
   Create  `applications` data bag having an empty data bag item per application, i.e.:


    ```
    
      data_bags/applications/my_app.json
                             cmr.json
                             api.json

    
    ```

Configure your Jenkins job:

* Authenticate with the Chef server by going to `<JENKINS_URL>/credential-store/domain/_/newCredentials`
* In your Jenkins job add a new build parameter: List Chef environments
    * Pick up the Chef server you authenticated against
    * click 'Save details to data bag item' and configure the fields as required.
    * add a new Build step where you must run chef-client on the remote hosts.
* Upon running the build the plugin will save the artifact details in the chosen data bag, e.g.:


```json

    {
      "id": "my_app",
      "dev": {                           <---------- Environment chosen in the dropdown
        "artifacts": [
          {
            "name": "myapp",
            "version": "1.8.1-SNAPSHOT"
          }
        ],
        "buildNumber" : "68",
        "environment" : "dev",
        "started" : "01-01-1921 13:02:36",
        "finished" : "",
        "duration" : "",
        "status" : ""
      }
    }

```

These details will be updated again once the build finishes (so it is a good idea to run your "run chef" Build step synchronously).


In your application cookbook, retrieve your application's artifacts:

```ruby

    artifacts = data_bag_item('applications', 'my_app')[node.chef_environment]['artifacts']

    artifacts.each do |name, version|
      # handle the deployment here
    end

```


### Build/Project action - Environment report (what got deployed where)

This is essentially the details captured by the build wrapper presented in a tabular form and grouped by environment.
The report is available for both individual builds and on project level.
