const listContainer = document.querySelector('#service-list');
let servicesRequest = new Request('/api/v1/services');
fetch(servicesRequest)
    .then(function (response) {
        return response.json();
    })
    .then(function (serviceList) {
        serviceList.forEach(service => {
            var li = document.createElement("li");
            li.appendChild(document.createTextNode('id: ' + service.id + ', name: ' + service.name
                + ', url: ' + service.url + ', status: ' + service.status + ', creation_date: ' + service.creation_date));
            listContainer.appendChild(li);
        });
    });

const saveButton = document.querySelector('#post-service');
saveButton.onclick = evt => {
    let url = document.querySelector('#url').value;
    let urlName = document.querySelector('#url-name').value;
    fetch('/api/v1/services', {
        method: 'post',
        headers: {
            'Accept': 'application/json, text/plain, */*',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({url: url, name: urlName})
    }).then(res => location.reload());
}